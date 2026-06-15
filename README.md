# BudgetBuddy

A comprehensive personal finance management Android application built for the OPSC6311 Final POE. BudgetBuddy helps users track expenses, manage budgets, analyse spending patterns, and stay motivated through gamification and savings challenges.

## Project Overview

BudgetBuddy is a Kotlin-based Android app that uses Room database for local persistence. It displays all currency values in South African Rand (ZAR) and targets API level 24+ devices. The app follows MVVM architecture with the Repository pattern, ViewBinding, Coroutines, and LiveData.

## Features

### Core Features (Part 1 & 2)
- User registration, login, and password reset
- Dashboard with budget overview
- Add, view, and delete expenses
- Category management (add/delete with duplicate prevention)
- Receipt image attachment
- South African Rand currency formatting

### Part 3 Features
- **Analytics Graph Screen** — PieChart, BarChart, and LineChart (MPAndroidChart) with date range filtering
- **Budget Progress Dashboard** — Monthly spending, remaining budget, percentage used, min/max goals, colour-coded status (Green/Yellow/Red)
- **Gamification System** — XP, levels, 7 badges, achievement history, badge gallery
- **Streak Tracking** — Daily, weekly, and best streaks stored in Room
- **Advanced Receipt Management** — Camera capture, gallery selection, FileProvider, full-screen viewer, delete/replace
- **Financial Insights Engine** — Highest/lowest category, average daily spend, monthly trends, warnings, savings suggestions
- **Savings Challenge System** — 4 challenges with progress tracking, completion rewards, and history

## Architecture

```
com.budgetbuddy/
├── ui/                  # Activities, adapters, ViewModels
├── data/                # Room DB, entities, DAOs, repositories, migrations
├── gamification/        # Badge definitions, GamificationManager
├── insights/            # InsightsManager
└── util/                # SessionManager, DateUtils, BudgetCalculator, CurrencyUtils
```

**Patterns used:**
- MVVM (ViewModel + LiveData)
- Repository Pattern
- Singleton database access
- Room migrations (v2 → v3, non-destructive)

## Technologies

| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| XML + ViewBinding | UI layouts |
| Room 2.6.1 | Local database |
| Material Design 3 | UI components |
| MPAndroidChart 3.1.0 | Analytics charts |
| Coroutines | Async operations |
| LiveData + ViewModel | MVVM state management |
| JUnit 4 + Mockito | Unit testing |
| GitHub Actions | CI/CD pipeline |

## Installation

1. Clone the repository:
   ```bash
   git clone <your-repo-url>
   cd "Budget Buddy"
   ```

2. Open the project in Android Studio (Hedgehog or newer recommended).

3. Let Gradle sync complete (requires internet for dependencies).

4. Connect an Android device (API 24+) or start an emulator.

5. Click **Run** or execute:
   ```bash
   ./gradlew installDebug
   ```

## Running Tests

```bash
./gradlew test
```

Test reports are generated at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

### Test Coverage
- Login & registration validation
- Category creation
- Expense creation
- Goal calculations (safe/warning/danger status)
- Analytics calculations
- Gamification system (badges, XP, levels)
- Challenge system (templates, progress)

## GitHub Actions

The CI pipeline (`.github/workflows/android.yml`) runs on every push and pull request:

1. Sets up JDK 17
2. Runs all unit tests
3. Builds the debug APK
4. Uploads APK and test results as artifacts
5. **Fails on any build or test error**

## APK Build Instructions

### Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Analytics Feature

Navigate: **Dashboard → Analytics**

- Select a date range using the From/To date pickers
- View spending breakdown by category (PieChart + BarChart)
- View spending trends over time (LineChart)
- See minimum and maximum budget goals
- Empty state shown when no data exists for the selected period

## Gamification Feature

Navigate: **Dashboard → Badges**

- Earn XP by logging expenses and unlocking badges
- 7 badges: First Expense, Expense Explorer, 7 Day Streak, 30 Day Streak, Budget Master, Savings Hero, Category Champion
- Level up as XP increases
- View achievement history and badge gallery
- Reward notifications shown when badges are unlocked

## Challenge Feature

Navigate: **Dashboard → Challenges**

| Challenge | Goal | Reward |
|---|---|---|
| Save R500 | Save at least R500 this month | 75 XP |
| Spend Less Than R1000 | Keep monthly spending under R1000 | 100 XP |
| Weekend Budget | Spend less than R500 over the weekend | 50 XP |
| No Fast-Food | Avoid Food expenses for 7 days | 60 XP |

## Database Schema (v3)

| Table | Purpose |
|---|---|
| users | Authentication |
| categories | Expense categories |
| expenses | Expense records with receipt URIs |
| budget_goals | Monthly budget, min/max goals per user |
| user_gamification | XP and level per user |
| user_badges | Unlocked badges per user |
| achievements | Achievement history |
| user_streaks | Daily/weekly/best streaks |
| challenges | Active and completed challenges |

Migration from v2 to v3 preserves all existing user data.

## Authors

- **Student Name** — OPSC6311 Final POE Submission
- **Institution** — Open Polytechnic of New Zealand

## License

This project is submitted as part of academic coursework for OPSC6311.
