# Cricket Tournament Management Module

## Product Requirements Document (PRD)

**Version:** 1.0\
**Module:** Tournament Management\
**Platform:** Cricket Mobile App\
**Primary Users:** Tournament Organizers, Team Managers/Captains, Players, Scorers, Viewers

---

# 1. Product Overview

The Tournament Management Module allows users to create, configure, manage, and follow complete cricket tournaments from a single application.

The module should not force every tournament into one fixed format. Instead, it should provide a **flexible tournament structure system** where an organizer can create different competition stages such as:

- Groups / Points Tables
- League Matches
- Individual Matches
- Series
- Knockouts
- Qualifiers
- Eliminators
- Quarter Finals
- Semi Finals
- Finals

A tournament can contain one or multiple stages.

For example:

```text
Tournament
│
├── Group A
│   ├── Teams
│   ├── Matches
│   └── Points Table
│
├── Group B
│   ├── Teams
│   ├── Matches
│   └── Points Table
│
├── Qualifier
│
├── Eliminator
│
├── Qualifier 2
│
└── Final
```

The system should support both:

1. **Manual tournament management**
2. **Automatic fixture/stage generation**

---

# 2. Product Goals

## Primary Goals

The system should allow an organizer to:

- Create a tournament
- Add and manage teams
- Add players through teams
- Create competition stages
- Assign teams to stages
- Schedule matches
- Generate fixtures automatically
- Edit generated fixtures
- Manage match progression
- Record match results
- Automatically update standings
- Automatically qualify teams for the next stage
- Track tournament statistics
- Share tournament information
- Allow users to follow tournament progress

---

# 3. Core Product Concept

The tournament system follows this hierarchy:

```text
Tournament
    ↓
Stages / Groups
    ↓
Teams
    ↓
Matches
    ↓
Results
    ↓
Standings / Qualification
    ↓
Next Stage
```

The tournament itself should be treated as a **container**.

A stage represents a specific competition structure inside that tournament.

---

# 4. User Roles

## 4.1 Tournament Organizer

Full control over the tournament.

Can:

- Create tournament
- Edit tournament
- Add/remove teams
- Create stages
- Manage groups
- Generate fixtures
- Schedule matches
- Reschedule matches
- Enter results
- Manage tournament rules
- Manage standings
- Manage qualification
- Share tournament
- Cancel tournament

---

## 4.2 Team Manager / Captain

Depending on organizer permissions:

- View tournament
- View team
- Add/manage players
- View fixtures
- Confirm participation
- View results
- View standings
- Receive notifications

---

## 4.3 Scorer

Can:

- Open assigned match
- Enter live score
- Record innings
- Record wickets
- Record extras
- Complete match
- Submit result

---

## 4.4 Player

Can:

- View tournaments
- View team
- View fixtures
- View results
- View statistics
- Follow tournament

---

## 4.5 Viewer

Can:

- View public tournaments
- View teams
- View fixtures
- View live matches
- View points table
- View statistics
- Share tournament

---

# 5. Tournament Lifecycle

A tournament follows this lifecycle:

```text
Draft
  ↓
Registration
  ↓
Setup
  ↓
Scheduled
  ↓
Live
  ↓
Completed
  ↓
Archived
```

## Draft

Tournament is being configured.

## Registration

Teams/players can be added.

## Setup

Organizer is creating groups, stages and fixtures.

## Scheduled

Fixtures have been created.

## Live

Tournament has started.

## Completed

Final match is completed and champion is determined.

## Archived

Tournament remains available for historical viewing.

---

# 6. Create Tournament

The organizer selects:

**Create Tournament**

## 6.1 Basic Information

Fields:

- Tournament Name
- Tournament Logo
- Cover Image
- Description
- Organizer Name
- Contact Information
- Location
- Start Date
- End Date

---

# 7. Tournament Type

The organizer can select the tournament category.

Examples:

- T20
- T10
- ODI
- Test
- Tape Ball
- Tennis Ball
- Hard Ball
- Indoor Cricket
- Custom

The tournament type should not unnecessarily control the competition structure.

For example:

```text
T20 + League
T20 + Knockout
T20 + Group + Playoffs
```

should all be possible.

---

# 8. Tournament Visibility

Options:

### Public

Anyone can discover the tournament.

### Private

Tournament is only accessible to invited users.

### Invite Only

Users can request access but organizer approves them.

### Tournament Code

Users can join using a unique code.

Example:

```text
JVV-4821
```

---

# 9. Tournament Teams

The tournament has a master team list.

Example:

```text
Tournament
│
├── BHH
├── NHH
├── Tigers
├── Lions
├── Eagles
└── Kings
```

Teams should exist independently from individual matches.

---

# 10. Add Team

Organizer can:

- Select existing team
- Create new team
- Invite team
- Add team using code

Team information:

- Team Name
- Logo
- Captain
- Vice Captain
- Manager
- Players

---

# 11. Team Registration Status

Each team can have a status:

```text
Pending
Approved
Rejected
Withdrawn
Active
Eliminated
Champion
Runner Up
```

---

# 12. Tournament Structure

This is the core feature.

The organizer can create multiple stages.

Example:

```text
Tournament
│
├── Group A
├── Group B
├── Qualifier 1
├── Eliminator
├── Qualifier 2
└── Final
```

Stages should be independent but connectable.

---

# 13. Create Stage

When organizer selects:

**Create Stage / Create Group**

show:

```text
Select Stage Type
```

## Stage Types

### Points Table / Group

Used for league competition.

### Match

Single match.

### Series Match

Multiple matches between teams.

### Knockout

Elimination-based competition.

### Qualifier

Special qualification match.

### Eliminator

Loser is eliminated.

### Quarter Final

Quarter-final stage.

### Semi Final

Semi-final stage.

### Final

Championship match.

---

# 14. Group / Points Table Stage

Example:

```text
GROUP A

BHH
NHH
Tigers
Lions
```

The group contains:

- Teams
- Matches
- Standings
- Rules
- Qualification settings

---

# 15. Group Settings

Organizer can configure:

### Number of Teams

Example:

```text
4
6
8
10
12
```

### Matches Per Team

Example:

```text
1
2
Custom
```

### Points System

Default:

```text
Win = 2
Tie = 1
No Result = 1
Loss = 0
```

Organizer can customize these values.

---

# 16. Qualification Rules

Organizer can specify:

```text
Top 1
Top 2
Top 4
Custom
```

Example:

```text
Group A
   ↓
Top 2 qualify
```

The system automatically identifies qualified teams when all required matches are completed.

---

# 17. Multiple Groups

Tournament can contain multiple groups.

Example:

```text
GROUP A

Team 1
Team 2
Team 3
Team 4
```

```text
GROUP B

Team 5
Team 6
Team 7
Team 8
```

Each group has its own:

- Matches
- Standings
- NRR
- Qualification

---

# 18. Group Sharing

Each group can have a share option.

Organizer can share:

- Group link
- Group code
- QR code

Users opening the link should be able to view the group.

---

# 19. Fixture Generation

The organizer can choose:

**Generate Fixtures**

The system generates matches according to the selected structure.

Example:

For 4 teams:

```text
A vs B
A vs C
A vs D
B vs C
B vs D
C vs D
```

---

# 20. Fixture Generation Options

Before generating fixtures:

- Number of matches per team
- Start date
- End date
- Match frequency
- Match time
- Match duration
- Venue
- Multiple venues
- Rest period
- Home/Away where applicable

---

# 21. Manual Scheduling

Automatic generation must not be mandatory.

Organizer can select:

**Schedule Match**

and manually create:

- Team A
- Team B
- Date
- Time
- Venue
- Officials
- Match type

---

# 22. Fixture Editing

Generated fixtures must remain editable.

Organizer can:

- Change date
- Change time
- Change venue
- Change teams
- Cancel match
- Postpone match
- Duplicate match
- Delete match

---

# 23. Match Status

Every match should have a status:

```text
Scheduled
Upcoming
Live
Completed
Postponed
Cancelled
Abandoned
No Result
```

---

# 24. Match Entity

Each match contains:

```text
Match
│
├── Stage
├── Team A
├── Team B
├── Date
├── Time
├── Venue
├── Officials
├── Status
├── Toss
├── Playing XI
├── Score
└── Result
```

---

# 25. Match Types

The system should distinguish between:

### Normal Match

```text
Team A vs Team B
```

### Series

```text
Team A vs Team B

Game 1
Game 2
Game 3
```

### Knockout Match

Loser is eliminated.

### Qualifier

Winner/loser follows predefined tournament progression.

### Eliminator

Loser is eliminated.

### Quarter Final

Winner progresses to Semi Final.

### Semi Final

Winner progresses to Final.

### Final

Winner becomes tournament champion.

---

# 26. Tournament Bracket

For knockout tournaments, the app should visually display:

```text
Quarter Finals
       ↓
Semi Finals
       ↓
Final
```

Example:

```text
QF1 ──┐
      ├── SF1 ──┐
QF2 ──┘         │
                ├── FINAL
QF3 ──┐         │
      ├── SF2 ──┘
QF4 ──┘
```

---

# 27. Automatic Match Progression

The system should automatically move winners forward.

Example:

```text
A vs B

A wins
 ↓
A automatically assigned to SF1
```

The organizer should not need to manually enter the winner again.

---

# 28. Qualifier + Eliminator Structure

The system should support advanced playoff structures.

Example:

```text
League
   ↓
Top 4

#1 ───── #2
 │
 └── Qualifier 1

#3 ───── #4
 │
 └── Eliminator

Qualifier 1 Loser
       +
Eliminator Winner
       ↓
Qualifier 2
       ↓
Final
```

---

# 29. Standings / Points Table

Each points-table stage gets its own standings.

Columns:

```text
Team
M
W
L
T
NR
P
NRR
```

Where:

- M = Matches
- W = Wins
- L = Losses
- T = Ties
- NR = No Result
- P = Points
- NRR = Net Run Rate

---

# 30. Automatic Standings Updates

After every completed match:

```text
Match Result
     ↓
Team Record Updated
     ↓
Points Updated
     ↓
NRR Updated
     ↓
Table Re-ranked
```

No manual calculation should normally be required.

---

# 31. Ranking Rules

Organizer can configure ranking priority.

Default:

```text
1. Points
2. NRR
3. Wins
4. Other configured tie-breaker
```

The system should support custom tie-break rules later.

---

# 32. Team Qualification

When a stage ends:

```text
Group A
   ↓
Standings
   ↓
Top 2
   ↓
Qualified
```

Qualified teams should automatically become eligible for the next configured stage.

---

# 33. Match Scheduling Between Stages

Example:

```text
Group A
Top 2
   ↓
Semi Final
```

The system should allow mapping:

```text
Group A #1 → SF1
Group A #2 → SF2
```

For multiple groups:

```text
Group A #1 → SF1
Group B #2 → SF1

Group B #1 → SF2
Group A #2 → SF2
```

---

# 34. Match Management

Organizer opens a match:

```text
BHH vs NHH

20 Aug 2026
4:00 PM
Main Ground
```

Available actions:

- Start Match
- Edit Match
- Postpone
- Cancel
- Enter Result
- View Scorecard
- Share Match

---

# 35. Match Result

After completion:

```text
BHH
145/6

NHH
138/8
```

Result:

```text
BHH won by 7 runs
```

The result automatically updates:

- Points table
- Team record
- Player statistics
- Tournament statistics
- Qualification
- Next stage

---

# 36. Tournament Dashboard

Tournament home should contain:

```text
Tournament Header

Tournament Name
Organizer
Dates
Location
Status
Share

START / SCHEDULE MATCH
CREATE STAGE
```

Tabs:

```text
HOME
TEAMS
MATCHES
POINTS
STATISTICS
```

---

# 37. Home Tab

Show:

- Tournament information
- Upcoming match
- Live match
- Recent result
- Teams
- Top players
- Tournament status

---

# 38. Teams Tab

Show all tournament teams.

Example:

```text
BHH
NHH
Tigers
Lions
```

Actions:

- View Team
- Add Team
- Remove Team

---

# 39. Matches Tab

Categories:

```text
Upcoming
Live
Completed
```

Example:

```text
BHH vs NHH
Today • 4:00 PM

Tigers vs Lions
Tomorrow • 7:00 PM
```

---

# 40. Points Tab

Show standings of active points-table stages.

If multiple groups:

```text
GROUP A
[Standings]

GROUP B
[Standings]
```

---

# 41. Statistics Tab

Tournament statistics:

### Batting

- Most Runs
- Highest Score
- Best Average
- Best Strike Rate
- Most Fours
- Most Sixes

### Bowling

- Most Wickets
- Best Bowling
- Best Economy
- Best Average

### Fielding

- Most Catches
- Most Run Outs
- Most Stumpings

---

# 42. Tournament Sharing

Tournament should have:



```text
Tournament ID:
JVV7212
```

#

---

# 44. Tournament Rules

Organizer can define:

- Overs
- Points
- Tie rules
- NRR rules
- Qualification
- Super Over rules
- Match duration
- Squad size
- Playing XI size
- Substitution rules

---

# 45. Tournament Settings

Settings should include:

### General

- Name
- Logo
- Description
- Dates
- Location

### Competition

- Tournament format
- Stage structure
- Points
- Qualification
- Ranking

### Teams

- Team limit
- Registration
- Approval

### Privacy

- Public/private
- Invite-only
- Tournament code

### Permissions

- Organizer
- Manager
- Scorer
- Viewer

---

# 46. Important UX Principle

The organizer should **not be forced to understand complicated tournament mathematics**.

The UI should guide them.

Instead of:

```text
Configure Stage Parameters
```

show:

```text
How do you want this stage to work?

○ Every team plays every other team
○ Teams play a fixed number of matches
○ Knockout
○ Custom
```

---

# 47. Recommended Tournament Creation Flow

```text
CREATE TOURNAMENT
        ↓
Basic Information
        ↓
Add Teams
        ↓
Competition Structure
        ↓
Create Stage
        ↓
Select Stage Type
        ↓
Configure Stage
        ↓
Assign Teams
        ↓
Generate / Schedule Matches
        ↓
Review Fixtures
        ↓
Publish Tournament
```

---

# 48. Recommended Flexible Structure

The app should NOT only have:

```text
Tournament Type = T20 / ODI
```

Instead it should have two separate concepts:

### Game Format

```text
T10
T20
ODI
Test
Custom
```

### Competition Structure

```text
League
Group
Knockout
League + Playoffs
Custom
```

This allows:

```text
T20 + League
T20 + Knockout
T20 + Group + Playoffs
T10 + League
T10 + Knockout
ODI + Group + Knockout
```

---

# 49. Recommended MVP

Do not build every advanced feature initially.

## Phase 1

Build:

```text
Create Tournament
        ↓
Add Teams
        ↓
Create Group
        ↓
Add Teams to Group
        ↓
Schedule Match
        ↓
Enter Result
        ↓
Points Table
```

---

# 50. Phase 2

Add:

```text
Automatic Fixture Generator
Multiple Groups
Qualification Rules
Knockout
Quarter Final
Semi Final
Final
Automatic Progression
Tournament Bracket
```

---

# 51. Phase 3

Add:

```text
Live Scoring
Player Statistics
Tournament Leaderboards
Notifications
Tournament Sharing
QR Codes
Multiple Venues
Officials
Advanced Scheduling
```

---

# 52. Phase 4

Advanced features:

```text
Online Team Registration
Registration Fees
Payments
Digital Player Registration
Tournament Chat
Announcements
Photos
Videos
Awards
Advanced Analytics
```

---

# 53. Complete Example

A complete tournament could look like:

```text
🏆 City Cricket Championship

12 Teams
T20

       ↓

GROUP A
4 Teams
League
       ↓
Top 2

GROUP B
4 Teams
League
       ↓
Top 2

GROUP C
4 Teams
League
       ↓
Top 2

       ↓

QUARTER FINALS
       ↓

SEMIFINALS
       ↓

FINAL
       ↓

🏆 CHAMPION
```

The system automatically handles:

```text
Teams
 ↓
Groups
 ↓
Fixtures
 ↓
Matches
 ↓
Results
 ↓
Points
 ↓
Qualification
 ↓
Knockout
 ↓
Final
 ↓
Champion
```

---

# 54. Core Data Relationship

Conceptually, the backend should follow:

```text
Tournament
    │
    ├── Teams
    │
    ├── Stages
    │     │
    │     ├── Stage Teams
    │     │
    │     ├── Matches
    │     │
    │     ├── Standings
    │     │
    │     └── Qualification Rules
    │
    ├── Venues
    │
    ├── Officials
    │
    └── Settings
```

And:

```text
Match
 │
 ├── Team A
 ├── Team B
 ├── Stage
 ├── Schedule
 ├── Result
 └── Scorecard
```

Later, the scorecard connects to:

```text
Match
 ↓
Innings
 ↓
Overs
 ↓
Balls
 ↓
Batting Events
 ↓
Bowling Events
 ↓
Player Statistics
```

---

# 55. Success Criteria

The Tournament Module is considered successful when an organizer can create a real tournament without needing external tools.

For example, an organizer should be able to:

1. Create a tournament.
2. Add 8 teams.
3. Create Group A.
4. Add 8 teams to the group.
5. Generate league fixtures.
6. Modify fixture dates.
7. Play matches.
8. Enter results.
9. Automatically update points.
10. Qualify top 4 teams.
11. Generate playoff matches.
12. Automatically move winners to the next stage.
13. Complete the final.
14. Declare the champion.
15. View tournament statistics.
16. Share the complete tournament with other users.

---

# 56. Final Product Vision

The final Tournament Module should behave like a **Tournament Builder + Tournament Manager + Competition Engine**.

The organizer should be able to start with:

```text
Create Tournament
```

and finish with:

```text
🏆 Tournament Completed
       ↓
Champion
       ↓
Final Standings
       ↓
Tournament Statistics
       ↓
Historical Record
```

The most important architectural principle is:

> **Do not hard-code one tournament format. Build a flexible Stage-based system.**

That way the same system can support:

```text
Simple 4-team League

8-team Knockout

10-team Round Robin

12-team Group Tournament

Group + Playoffs

Qualifier + Eliminator + Final

Multi-stage Custom Tournament
```

without creating a separate system for every tournament type.
