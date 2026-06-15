# BudgetBuddy Submission Checklist

Use this document during marking/demo to verify all required deliverables quickly.

---

## 1) Project Compliance

- [x] Android app built in Kotlin
- [x] XML-based UI layouts
- [x] Minimum SDK/API level is 24
- [x] Local persistence implemented with RoomDB
- [x] Currency displayed in South African Rand (R)
- [x] Multi-screen app with working navigation

---

## 2) Functional Requirements Mapping

### Authentication

- [x] **Login page works**
  - Valid email + password logs user in and opens dashboard
  - Invalid credentials are rejected with feedback
- [x] **Forgot password works**
  - Existing email can reset password
  - Non-existent email shows error
- [x] **Sign up / register works**
  - New user registration succeeds
  - Duplicate email is blocked
  - Basic validations included (email format, password length, password confirmation)

### Dashboard

- [x] Dashboard present and functional
- [x] Shows monthly budget value
- [x] Shows spent total (from RoomDB expenses)
- [x] Shows remaining budget
- [x] Includes progress indicator for spending
- [x] Provides navigation actions to Add Expense, Category Management, and Expense List

### Add Expense

- [x] Add expense screen present and functional
- [x] User can enter amount, date, category, note
- [x] User can optionally upload/select receipt image
- [x] Save writes expense into RoomDB
- [x] Validations included (required fields, amount > 0)

### Category Management

- [x] Category management screen present and functional
- [x] Add category works
- [x] Delete category works
- [x] Duplicate categories are prevented
- [x] Default categories are seeded (Food, Transport, Entertainment)

### Expense List

- [x] Expense list screen present and functional
- [x] Displays saved expenses from RoomDB
- [x] Shows amount, date, category, and note
- [x] Empty-state message appears when no expenses exist

---

## 3) RoomDB Requirements

- [x] Room entities defined: `User`, `Category`, `Expense`
- [x] DAO layer implemented for CRUD/query operations
- [x] Single database entry point: `AppDatabase`
- [x] Repository pattern used via `BudgetRepository`
- [x] Data persists across app restarts

---

## 4) UI/UX Quality Checklist

- [x] Clean XML layouts per screen
- [x] Consistent design language (colors, cards, buttons, spacing)
- [x] Dashboard and feature pages visually aligned with provided mockup style
- [x] Input validations and user feedback via Toast messages
- [x] Empty states and edge cases handled

---

## 5) Demo Script (Presentation Ready)

Follow these steps in order for a smooth graded demonstration.

1. Launch app -> login screen appears.
2. Tap **New user? Register** and create an account.
3. Return/login with newly created credentials.
4. On dashboard, show:
   - Monthly budget in Rand
   - Spent/Remaining values
   - Navigation action tiles
5. Open **Category Management**:
   - Add a new category (e.g., `Health`)
   - Attempt duplicate category to show validation
   - Delete category (optional)
6. Open **Add Expense**:
   - Enter amount, choose date, choose category, add note
   - Optionally select receipt image
   - Save expense
7. Return to **Dashboard** and confirm spent/remaining updates.
8. Open **Expense List** and confirm new record appears.
9. Open **Forgot Password**:
   - Reset password for existing email
   - Show login works with the new password
10. Logout and log in again to confirm full flow integrity.

---

## 6) Quick Marker Notes

- Architecture uses Activity-based navigation + Room repository.
- Designed for API 24+ devices.
- Core requirements implemented end-to-end and tested through UI flow.
