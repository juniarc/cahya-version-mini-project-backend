# Auth Project – Employee Timesheet REST API

## Summary

REST API for an Employee Timesheet Management System built with Spring Boot, Oracle DB and JWT. Users register and log in, manage their employee profile, and submit timesheets through a maker–checker workflow (the maker creates a timesheet, the assigned checker approves or rejects it). Admins can view all timesheets.

- **Base URL:** `http://localhost:8081`
- **Format:** JSON (`Content-Type: application/json`)
- **Auth:** JWT Bearer token (valid 24 hours). Get it from `/api/auth/login`, then send `Authorization: Bearer <accessToken>` on every endpoint except `/api/auth/**`.
- **Roles:** `user` (default) and `admin` (only for `/timesheet/admin`).
- **Errors:** business errors return `400` with `{ "error": "message" }`; missing/invalid token or insufficient role returns `403`.

---

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Log in and receive a JWT |
| GET | `/me/employee` | Token | Get own employee profile |
| PUT | `/me/employee/edit` | Token | Update own employee profile |
| GET | `/employee/all` | Token | List all employees |
| GET | `/employee/all/summary` | Token | List employees (id and name only) |
| POST | `/timesheet/create` | Token | Create a timesheet |
| GET | `/timesheet/all` | Token | List timesheets I created (maker) |
| GET | `/timesheet/checker/all` | Token | List timesheets assigned to me (checker) |
| GET | `/timesheet/{id}` | Token | Get a timesheet by ID |
| PUT | `/timesheet/maker/edit/{id}` | Token (maker) | Edit timesheet content |
| PUT | `/timesheet/checker/edit/{id}` | Token (checker) | Approve/reject a timesheet |
| DELETE | `/timesheet/delete/{id}` | Token (maker) | Delete a timesheet |
| GET | `/timesheet/admin` | Token (`admin`) | List all timesheets |

---

### POST `/api/auth/register`

Creates a user with the `user` role and a linked employee record.

| Field | Type | Required | Rules |
|---|---|---|---|
| `name` | string | Yes | Not blank |
| `username` | string | Yes | Not blank, unique |
| `email` | string | Yes | Valid email, unique |
| `password` | string | Yes | Min. 6 characters |

```json
{ "name": "Jane Doe", "username": "janedoe", "email": "jane@example.com", "password": "secret123" }
```

**201 Created**
```json
{
  "message": "Register successfully",
  "user": { "id": 1, "name": "Jane Doe", "username": "janedoe", "email": "jane@example.com", "password": null, "roles": [{ "roleId": 2, "roleName": "user" }] }
}
```
**400:** `Username is already taken.` · `Email is already taken` · `Role not found.`

---

### POST `/api/auth/login`

| Field | Type | Required |
|---|---|---|
| `username` | string | Yes |
| `password` | string | Yes |

**200 OK**
```json
{ "message": "Login successfully", "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
```
**400:** `Bad credentials`

---

### GET `/me/employee`

Returns the authenticated user's employee profile.

**200 OK**
```json
{
  "employee": {
    "empId": 1, "empName": "Jane Doe", "empRole": "Developer", "empEmail": "jane@example.com",
    "dateOfJoin": "2025-01-15T00:00:00.000+00:00", "empPhone": "+62812345678",
    "created_at": "2026-11-24T08:00:00.000+00:00", "updated_at": "2026-11-24T08:00:00.000+00:00",
    "userId": 1, "manager": { "empId": 5, "empName": "John Manager" }
  }
}
```
**400:** `Employee not found.`

---

### PUT `/me/employee/edit`

| Field | Type | Required | Notes |
|---|---|---|---|
| `empName` | string | Yes | |
| `empEmail` | string | Yes | Valid email |
| `empRole` | string | Yes | Job title |
| `empPhone` | string | Yes | |
| `dateOfJoin` | date | Yes | `yyyy-MM-dd` |
| `managerId` | number | Yes | `empId` of the manager |

```json
{ "empName": "Jane Doe", "empEmail": "jane@example.com", "empRole": "Developer", "empPhone": "+62812345678", "dateOfJoin": "2025-01-15", "managerId": 5 }
```

**200 OK**
```json
{ "message": "Employee updated successfully.", "employee": { "empId": 1, "empName": "Jane Doe", "...": "..." } }
```
**400:** `Employee not found.` · `Manager not found.`

---

### GET `/employee/all`

**200 OK**
```json
{ "message": "Get all employee successfully.", "employee": [ { "empId": 1, "empName": "Jane Doe", "empRole": "Developer", "...": "..." } ] }
```

---

### GET `/employee/all/summary`

**200 OK**
```json
{ "message": "Get all employee successfully.", "employee": [ { "empId": 1, "empName": "Jane Doe" }, { "empId": 2, "empName": "John Manager" } ] }
```

---

### POST `/timesheet/create`

The caller becomes the maker. Status starts as `Pending`.

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | Yes | Project name |
| `leadId` | number | Yes | `empId` of project lead |
| `location` | string | Yes | Work location |
| `remarks` | string | No | Remarks |
| `checkerId` | number | Yes | `empId` of reviewer |

```json
{ "name": "Website Redesign", "leadId": 2, "location": "Jakarta Office", "remarks": "Sprint 4 work", "checkerId": 5 }
```

**201 Created**
```json
{
  "message": "Timesheet created successfully.",
  "timesheet": {
    "id": 10, "pname": "Website Redesign", "plead": { "empId": 2, "...": "..." },
    "location": "Jakarta Office", "remarks": "Sprint 4 work",
    "maker": { "empId": 1, "...": "..." }, "checker": { "empId": 5, "...": "..." },
    "checkerStatus": "Pending", "checkerRemark": null, "submitDate": "2026-11-24", "checkerSubmitDate": null
  }
}
```
**400:** `Employee not found.` · `Employee leader not found.` · `Employee checker not found.`

---

### GET `/timesheet/all`

Timesheets where the caller is the maker.

**200 OK**
```json
{
  "timesheetList": [
    {
      "id": 10, "pname": "Website Redesign",
      "plead": { "empId": 2, "empName": "John Lead" },
      "location": "Jakarta Office", "remarks": "Sprint 4 work",
      "maker": { "empId": 1, "empName": "Jane Doe" },
      "checker": { "empId": 5, "empName": "John Manager" },
      "checkerStatus": "Pending", "checkerRemark": null, "checkerSubmitDate": null,
      "submitDate": "2026-11-24T08:00:00.000+00:00"
    }
  ]
}
```

---

### GET `/timesheet/checker/all`

Timesheets where the caller is the assigned checker. Same response shape as `/timesheet/all`.

---

### GET `/timesheet/{id}`

| Path param | Type | Description |
|---|---|---|
| `id` | number | Timesheet ID |

**200 OK** (reduced view: no `maker`, status or checker remark)
```json
{
  "timesheet": {
    "id": 10, "pname": "Website Redesign", "plead": { "empId": 2, "empName": "John Lead" },
    "location": "Jakarta Office", "remarks": "Sprint 4 work",
    "checker": { "empId": 5, "empName": "John Manager" }, "submitDate": "2026-11-24T08:00:00.000+00:00"
  }
}
```
**400:** `Timesheet not found.`

---

### PUT `/timesheet/maker/edit/{id}`

Maker only. All fields required.

| Field | Type | Description |
|---|---|---|
| `name` | string | Project name |
| `leadId` | number | `empId` of project lead |
| `location` | string | Work location |
| `remarks` | string | Remarks |
| `checkerId` | number | `empId` of reviewer |

**200 OK**
```json
{ "message": "Timesheet updated successfully.", "timesheet": { "id": 10, "pname": "Website Redesign v2", "...": "..." } }
```
**400:** `Timesheet not found.` · `Employee checker not found.` · `You do not have permission to edit this data.`

---

### PUT `/timesheet/checker/edit/{id}`

Assigned checker only.

| Field | Type | Required | Description |
|---|---|---|---|
| `checkerStatus` | string | Yes | e.g. `Approved` / `Rejected` (free text) |
| `checkerRemark` | string | No | Reviewer comment |

```json
{ "checkerStatus": "Approved", "checkerRemark": "Looks good" }
```

**200 OK**
```json
{ "message": "Timesheet updated successfully.", "timesheet": { "id": 10, "checkerStatus": "Approved", "checkerRemark": "Looks good", "checkerSubmitDate": "2026-11-24", "...": "..." } }
```
**400:** `Timesheet not found.` · `You do not have permission to edit this data.`

---

### DELETE `/timesheet/delete/{id}`

Maker only.

**200 OK**
```json
{ "message": "Timesheet deleted successfully." }
```
**400:** `Timesheet not found.` · `You do not have permission to edit this data.`

---

### GET `/timesheet/admin`

Requires the `admin` role.

**200 OK**
```json
{
  "timesheetList": [
    { "maker": "Jane Doe", "checker": "John Manager", "status": "Approved", "checkerRemark": "Looks good", "submitDate": "2026-11-24T08:00:00.000+00:00" }
  ]
}
```
**403:** caller is not an admin.