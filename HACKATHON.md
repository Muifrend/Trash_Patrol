# Trash Patrol — hackathon case study

[Project overview](README.md) · [Recorded demo](https://www.youtube.com/shorts/33VFazESk8A) · [Published results](https://www.gmicloud.ai/en/blog/what-32-teams-built-for-the-world-cup-in-one-day)

## Event and result

Trash Patrol competed as **Team Spike (Tunisia)** at the **Google I/O Kickoff: Pre-World Cup Hack** on **May 23, 2026**.

The [GMI Cloud recap](https://www.gmicloud.ai/en/blog/what-32-teams-built-for-the-world-cup-in-one-day) records **first place among 32 teams**, **490/600 points** across ten judges, and a **$2,000 team prize**.

## Contribution

**Andrzej Firek was the primary developer**, implementing the Android app, backend orchestration, LEGO bridge, and integration. **Stephen Kelly and Rafael Sánchez contributed to ideation.**

## Product idea

Make litter reporting quick, give the user visible progress while a robot acts, and verify that the reported trash is gone. The World Cup theme turns each cleanup into a match, with the robot as a striker and a successful verification as a goal.

## Implemented system

The system has three components:

1. **Android app:** Kotlin and Jetpack Compose capture report and confirmation photos, attach optional GPS metadata, and display mission state on a soccer-themed interface.
2. **Backend:** FastAPI manages an in-memory mission state machine and calls Gemini through GMI Cloud directly for image classification and cleanup verification. RocketRide runs the robot-dispatch pipeline.
3. **Robot bridge:** a laptop HTTP API translates commands into three-byte Bluetooth messages for a LEGO SPIKE Prime hub running Pybricks. The hub controls the drive motors, ultrasonic sensor, and sweeper motor.

The source and setup links are in the [component table](README.md#code-across-three-repositories).

## Report → act → verify

A report photo is classified before dispatch. If trash is confirmed, RocketRide sends `SEARCH_AND_SWEEP` to the bridge. The robot performs its local search/sweep routine and returns a result.

A completion result prompts the user for a new photo. The backend then asks Gemini whether the trash has been removed. This separates the robot's own completion report from the visual verification step.

## Engineering decisions

- **One mission ID across components** connects the app's status view to classification, dispatch, and verification.
- **A hardware bridge** keeps Bluetooth and hub-specific code outside the mobile app and backend.
- **A separate confirmation photo** closes the loop after physical action.
- **Mock bridge mode** supports HTTP integration work without a connected LEGO robot.

## What the prototype demonstrates

The hackathon demo connects mobile input, model inference, workflow dispatch, and physical robot control in one flow. It demonstrates local robot cleanup rather than city-scale autonomous deployment.

GPS is report metadata, not a navigation target. The pitch animation represents mission progress, not robot localization. Missions are stored in memory; timeout handling and unauthenticated demo APIs remain limitations. See [prototype scope](README.md#prototype-scope) for the current boundaries.

## Technologies

| Area | Technologies |
| --- | --- |
| Mobile | Kotlin, Jetpack Compose, Material 3, coroutines / StateFlow, Retrofit / OkHttp |
| Backend | Python, FastAPI, RocketRide |
| Vision | Google Gemini served through GMI Cloud |
| Hardware | LEGO SPIKE Prime, Pybricks, Bluetooth Low Energy, Bleak |
