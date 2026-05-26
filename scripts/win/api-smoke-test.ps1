param(
    [string]$BaseUrl = "http://localhost",
    [string]$StudentServiceUrl = "http://localhost:8081",
    [string]$CourseServiceUrl = "http://localhost:8082",
    [string]$TeacherServiceUrl = "http://localhost:8083"
)

$ErrorActionPreference = "Stop"

function New-TestBody {
    param([hashtable]$Body)
    return ($Body | ConvertTo-Json -Depth 10)
}

function Read-ErrorBody {
    param($Response)
    if ($null -eq $Response) {
        return $null
    }
    try {
        $stream = $Response.GetResponseStream()
        if ($null -eq $stream) {
            return $null
        }
        $reader = [System.IO.StreamReader]::new($stream)
        $content = $reader.ReadToEnd()
        if ([string]::IsNullOrWhiteSpace($content)) {
            return $null
        }
        return $content | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Invoke-Api {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [string]$Token = "",
        [int]$ExpectedHttpStatus = 200,
        [Nullable[int]]$ExpectedResultCode = 200,
        [int]$MaxAttempts = 6,
        [int]$RetryDelaySeconds = 5
    )

    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $headers
        ContentType = "application/json"
        UseBasicParsing = $true
    }

    if ($null -ne $Body) {
        $params.Body = New-TestBody $Body
    }

    $status = 0
    $payload = $null
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            $response = Invoke-WebRequest @params
            $status = [int]$response.StatusCode
            $payload = $null
            if (-not [string]::IsNullOrWhiteSpace($response.Content)) {
                $payload = $response.Content | ConvertFrom-Json
            }
        } catch {
            $status = [int]$_.Exception.Response.StatusCode
            $payload = Read-ErrorBody $_.Exception.Response
        }

        if ($status -ne 503 -or $ExpectedHttpStatus -eq 503 -or $attempt -eq $MaxAttempts) {
            break
        }

        Write-Host "[WAIT] $Name returned 503, retrying in $RetryDelaySeconds seconds ($attempt/$MaxAttempts)"
        Start-Sleep -Seconds $RetryDelaySeconds
    }

    if ($status -ne $ExpectedHttpStatus) {
        throw "[$Name] HTTP status expected $ExpectedHttpStatus, actual $status. Body: $($payload | ConvertTo-Json -Depth 10 -Compress)"
    }

    if ($null -ne $ExpectedResultCode) {
        if ($null -eq $payload -or $payload.code -ne $ExpectedResultCode) {
            throw "[$Name] Result code expected $ExpectedResultCode, actual $($payload.code). Body: $($payload | ConvertTo-Json -Depth 10 -Compress)"
        }
    }

    Write-Host "[PASS] $Name"
    return $payload
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

$runId = Get-Date -Format "yyyyMMddHHmmss"
$adminLogin = Invoke-Api `
    -Name "admin login" `
    -Method "POST" `
    -Url "$BaseUrl/api/auth/login" `
    -Body @{ username = "admin"; password = "admin123" }

$adminToken = $adminLogin.data.token
Assert-True (-not [string]::IsNullOrWhiteSpace($adminToken)) "Admin token is empty."

Invoke-Api -Name "public course list" -Method "GET" -Url "$BaseUrl/api/courses" -ExpectedResultCode 200 | Out-Null
Invoke-Api -Name "students list requires token" -Method "GET" -Url "$BaseUrl/api/students" -ExpectedHttpStatus 401 -ExpectedResultCode $null | Out-Null
Invoke-Api -Name "admin student list" -Method "GET" -Url "$BaseUrl/api/students" -Token $adminToken | Out-Null
Invoke-Api -Name "admin teacher list" -Method "GET" -Url "$BaseUrl/api/teachers" -Token $adminToken | Out-Null
Invoke-Api -Name "admin enrollment list" -Method "GET" -Url "$BaseUrl/api/enrollments" -Token $adminToken | Out-Null

$course = Invoke-Api `
    -Name "create course" `
    -Method "POST" `
    -Url "$BaseUrl/api/courses" `
    -Token $adminToken `
    -Body @{
        courseCode = "API-$runId"
        courseName = "API Smoke Course $runId"
        credit = 2.0
        capacity = 2
        status = "OPEN"
        description = "Created by scripts/win/api-smoke-test.ps1"
        schedule = @{
            startWeek = 1
            endWeek = 16
            weekType = "ALL"
            weekday = 6
            startSection = 3
            endSection = 4
            startTime = "09:00:00"
            endTime = "10:00:00"
            classroom = "API-$runId"
        }
    }
$courseId = [int64]$course.data.courseId
Assert-True ($courseId -gt 0) "Course id is invalid."

Invoke-Api `
    -Name "update course" `
    -Method "PUT" `
    -Url "$BaseUrl/api/courses/$courseId" `
    -Token $adminToken `
    -Body @{
        courseName = "API Smoke Course Updated $runId"
        credit = 3.0
        capacity = 3
        status = "OPEN"
        description = "Updated by API smoke test"
    } | Out-Null

Invoke-Api `
    -Name "create course schedule" `
    -Method "POST" `
    -Url "$BaseUrl/api/courses/$courseId/schedules" `
    -Token $adminToken `
    -Body @{
        startWeek = 1
        endWeek = 16
        weekType = "ALL"
        weekday = 7
        startSection = 5
        endSection = 6
        startTime = "13:45:00"
        endTime = "15:30:00"
        classroom = "API-$runId-EXTRA"
    } | Out-Null

$schedules = Invoke-Api -Name "query course schedules" -Method "GET" -Url "$BaseUrl/api/courses/$courseId/schedules" -Token $adminToken
Assert-True (($schedules.data | Measure-Object).Count -ge 1) "Course schedule was not created."

$retryIndex = 0
$preAssigned = Invoke-Api -Name "precheck teacher assignment" -Method "GET" -Url "$TeacherServiceUrl/internal/courses/$courseId/teacher-assigned"
while ($preAssigned.data.assigned -eq $true -and $retryIndex -lt 10) {
    $retryIndex++
    $course = Invoke-Api `
        -Name "create fallback course $retryIndex" `
        -Method "POST" `
        -Url "$BaseUrl/api/courses" `
        -Token $adminToken `
        -Body @{
            courseCode = "API-$runId-R$retryIndex"
            courseName = "API Smoke Course $runId Retry $retryIndex"
            credit = 3.0
            capacity = 3
            status = "OPEN"
            description = "Created after stale assignment precheck"
            schedule = @{
                startWeek = 1
                endWeek = 16
                weekType = "ALL"
                weekday = 6
                startSection = 3
                endSection = 4
                startTime = "09:00:00"
                endTime = "10:00:00"
                classroom = "API-$runId-R$retryIndex"
            }
        }
    $courseId = [int64]$course.data.courseId
    Invoke-Api `
        -Name "create fallback course schedule $retryIndex" `
        -Method "POST" `
        -Url "$BaseUrl/api/courses/$courseId/schedules" `
        -Token $adminToken `
        -Body @{
            startWeek = 1
            endWeek = 16
            weekType = "ALL"
            weekday = 7
            startSection = 5
            endSection = 6
            startTime = "13:45:00"
            endTime = "15:30:00"
            classroom = "API-$runId-R$retryIndex-EXTRA"
        } | Out-Null
    $preAssigned = Invoke-Api -Name "precheck fallback teacher assignment $retryIndex" -Method "GET" -Url "$TeacherServiceUrl/internal/courses/$courseId/teacher-assigned"
}
Assert-True ($preAssigned.data.assigned -ne $true) "Could not find an unassigned test course after $retryIndex retries."

$teacherUsername = "teacher$runId"
$teacherPassword = "pass$runId"
$teacher = Invoke-Api `
    -Name "create teacher" `
    -Method "POST" `
    -Url "$BaseUrl/api/teachers" `
    -Token $adminToken `
    -Body @{
        teacherNo = "T$runId"
        username = $teacherUsername
        password = $teacherPassword
        name = "API Teacher $runId"
        title = "Lecturer"
        phone = "13900000000"
        email = "teacher$runId@example.com"
        status = "ACTIVE"
    }
$teacherId = [int64]$teacher.data.teacherId
Assert-True ($teacherId -gt 0) "Teacher id is invalid."

$teacherLogin = Invoke-Api `
    -Name "teacher login" `
    -Method "POST" `
    -Url "$BaseUrl/api/auth/login" `
    -Body @{ username = $teacherUsername; password = $teacherPassword }
$teacherToken = $teacherLogin.data.token
Assert-True (-not [string]::IsNullOrWhiteSpace($teacherToken)) "Teacher token is empty."
Assert-True ($teacherLogin.data.role -eq "TEACHER") "Teacher login role is invalid."
Assert-True ([int64]$teacherLogin.data.relatedId -eq $teacherId) "Teacher login relatedId does not match created teacher id."

Invoke-Api `
    -Name "update teacher" `
    -Method "PUT" `
    -Url "$BaseUrl/api/teachers/$teacherId" `
    -Token $adminToken `
    -Body @{
        name = "API Teacher Updated $runId"
        title = "Professor"
        phone = "13900000001"
        email = "teacher-updated$runId@example.com"
        status = "ACTIVE"
    } | Out-Null

Invoke-Api -Name "assign teacher to course" -Method "POST" -Url "$BaseUrl/api/teachers/$teacherId/courses/$courseId" -Token $adminToken | Out-Null
$teacherCourses = Invoke-Api -Name "query teacher courses" -Method "GET" -Url "$BaseUrl/api/teachers/$teacherId/courses" -Token $adminToken
Assert-True (($teacherCourses.data | Where-Object { $_.courseId -eq $courseId } | Measure-Object).Count -ge 1) "Teacher-course assignment was not found."

$teacherAssigned = Invoke-Api -Name "internal teacher assigned" -Method "GET" -Url "$TeacherServiceUrl/internal/courses/$courseId/teacher-assigned"
Assert-True ($teacherAssigned.data.assigned -eq $true) "Internal teacher-assigned check failed."

$studentUsername = "student$runId"
$studentPassword = "pass$runId"
$register = Invoke-Api `
    -Name "student register" `
    -Method "POST" `
    -Url "$BaseUrl/api/auth/register" `
    -Body @{
        studentNo = "S$runId"
        name = "API Student $runId"
        major = "Software Engineering"
        grade = "2026"
        phone = "13800000000"
        email = "student$runId@example.com"
        username = $studentUsername
        password = $studentPassword
    }
$studentId = [int64]$register.data.studentId
Assert-True ($studentId -gt 0) "Registered student id is invalid."

$studentLogin = Invoke-Api `
    -Name "student login" `
    -Method "POST" `
    -Url "$BaseUrl/api/auth/login" `
    -Body @{ username = $studentUsername; password = $studentPassword }
$studentToken = $studentLogin.data.token
Assert-True (-not [string]::IsNullOrWhiteSpace($studentToken)) "Student token is empty."
Assert-True ([int64]$studentLogin.data.relatedId -eq $studentId) "Student login relatedId does not match registered student id."

Invoke-Api -Name "student query own detail" -Method "GET" -Url "$BaseUrl/api/students/$studentId" -Token $studentToken | Out-Null
Invoke-Api `
    -Name "student update own profile" `
    -Method "PUT" `
    -Url "$BaseUrl/api/students/$studentId" `
    -Token $studentToken `
    -Body @{
        name = "API Student Updated $runId"
        major = "Computer Science"
        grade = "2026"
        phone = "13800000001"
        email = "student-updated$runId@example.com"
    } | Out-Null

Invoke-Api -Name "student cannot list all students" -Method "GET" -Url "$BaseUrl/api/students" -Token $studentToken -ExpectedHttpStatus 403 -ExpectedResultCode $null | Out-Null
Invoke-Api `
    -Name "student cannot create course" `
    -Method "POST" `
    -Url "$BaseUrl/api/courses" `
    -Token $studentToken `
    -ExpectedHttpStatus 403 `
    -ExpectedResultCode $null `
    -Body @{
        courseCode = "DENY-$runId"
        courseName = "Denied"
        credit = 1.0
        capacity = 1
        status = "OPEN"
        description = "Should be blocked by gateway"
        schedule = @{
            startWeek = 1
            endWeek = 16
            weekType = "ALL"
            weekday = 1
            startSection = 1
            endSection = 2
            startTime = "08:00:00"
            endTime = "09:45:00"
            classroom = "DENY-$runId"
        }
    } | Out-Null

$studentStatus = Invoke-Api -Name "internal student status" -Method "GET" -Url "$StudentServiceUrl/internal/students/$studentId/status"
Assert-True ($studentStatus.data.active -eq $true) "Internal student status check failed."

$capacity = Invoke-Api -Name "internal course capacity" -Method "GET" -Url "$CourseServiceUrl/internal/courses/$courseId/check-capacity"
Assert-True ($capacity.data.selectable -eq $true -and $capacity.data.hasCapacity -eq $true) "Internal course capacity check failed."

$internalSchedule = Invoke-Api -Name "internal course schedule" -Method "GET" -Url "$CourseServiceUrl/internal/courses/$courseId/schedule"
Assert-True (($internalSchedule.data | Measure-Object).Count -ge 1) "Internal course schedule is empty."

$enrollment = Invoke-Api `
    -Name "student enroll course" `
    -Method "POST" `
    -Url "$BaseUrl/api/enrollments" `
    -Token $studentToken `
    -Body @{ studentId = $studentId; courseId = $courseId }
$enrollmentId = [int64]$enrollment.data.enrollmentId
Assert-True ($enrollmentId -gt 0) "Enrollment id is invalid."

Invoke-Api `
    -Name "duplicate enrollment rejected" `
    -Method "POST" `
    -Url "$BaseUrl/api/enrollments" `
    -Token $studentToken `
    -Body @{ studentId = $studentId; courseId = $courseId } `
    -ExpectedHttpStatus 409 `
    -ExpectedResultCode 409 | Out-Null

$studentEnrollments = Invoke-Api -Name "student enrollment records" -Method "GET" -Url "$BaseUrl/api/enrollments/students/$studentId" -Token $studentToken
Assert-True (($studentEnrollments.data | Where-Object { $_.enrollmentId -eq $enrollmentId } | Measure-Object).Count -ge 1) "Student enrollment record was not found."

$timetable = Invoke-Api -Name "student timetable" -Method "GET" -Url "$BaseUrl/api/enrollments/students/$studentId/timetable" -Token $studentToken
Assert-True (($timetable.data | Where-Object { $_.courseId -eq $courseId } | Measure-Object).Count -ge 1) "Timetable does not contain enrolled course."
Assert-True (($timetable.data | Where-Object { $_.courseId -eq $courseId -and $_.startWeek -eq 1 -and $_.endWeek -eq 16 -and $_.weekType -eq "ALL" } | Measure-Object).Count -ge 1) "Timetable does not contain week metadata."

$weekTimetable = Invoke-Api -Name "student week timetable" -Method "GET" -Url "$BaseUrl/api/enrollments/students/$studentId/timetable?weekNo=6" -Token $studentToken
Assert-True (($weekTimetable.data | Where-Object { $_.courseId -eq $courseId } | Measure-Object).Count -ge 1) "Week timetable does not contain enrolled course."

$adminEnrollments = Invoke-Api -Name "admin enrollment records" -Method "GET" -Url "$BaseUrl/api/enrollments?courseId=$courseId&pageSize=100" -Token $adminToken
Assert-True (($adminEnrollments.data.records | Where-Object { $_.enrollmentId -eq $enrollmentId } | Measure-Object).Count -ge 1) "Admin enrollment list does not contain smoke test enrollment."

$teacherCourseStudents = Invoke-Api -Name "teacher course students" -Method "GET" -Url "$BaseUrl/api/enrollments/teachers/$teacherId/courses/$courseId/students" -Token $teacherToken
Assert-True (($teacherCourseStudents.data | Where-Object { $_.studentId -eq $studentId } | Measure-Object).Count -ge 1) "Teacher course student list does not contain enrolled student."

Invoke-Api -Name "student drop course" -Method "DELETE" -Url "$BaseUrl/api/enrollments/$enrollmentId" -Token $studentToken | Out-Null
$afterDrop = Invoke-Api -Name "student enrollments after drop" -Method "GET" -Url "$BaseUrl/api/enrollments/students/$studentId" -Token $studentToken
Assert-True (($afterDrop.data | Where-Object { $_.enrollmentId -eq $enrollmentId -and $_.status -eq "DROPPED" } | Measure-Object).Count -eq 1) "Dropped enrollment status was not found."

Invoke-Api -Name "cancel teacher assignment" -Method "DELETE" -Url "$BaseUrl/api/teachers/$teacherId/courses/$courseId" -Token $adminToken | Out-Null

Write-Host ""
Write-Host "API smoke test completed successfully."
Write-Host "Created test data: courseId=$courseId, teacherId=$teacherId, studentId=$studentId, enrollmentId=$enrollmentId"
