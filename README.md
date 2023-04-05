# SubTick

[![License](https://img.shields.io/github/license/Fallen-Breath/fabric-mod-template.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)

This mod uses Fallen's fabric mod template.

A carpet extension that allows you to freeze and step to any specific tick phase, as well as step through tile ticks, fluid ticks, block events, entities, and block entities individually.

## Current Command Layout (subject to change)

[] represents an optional argument, and <> represents an obligatory argument. If an argument is written like `count=1`, that means 1 is the default value.

- `tick freeze [phase=subtickDefaultPhase]`: Freezes at the specified phase, defaulting to subtickDefaultPhase (carpet rule).
- `tick step [count=1] [phase=subtickDefaultPhase]`: Steps the specified number of ticks, ending right before the specified phase. Supports `tick step 0 [phase]` to step to a different phase in the same tick.
- `phaseStep [count=1]`: Steps the specified number of phases forward, **stepping to the next tick if necessary**.
- `phaseStep [phase]`: Steps to the next instance of `phase` **within the current tick**.
- `phaseStep [phase] force`: Steps to the next instance of `phase` **stepping to the next tick if necessary**.
- `queueStep <queue> [count=1] [radius]`: Steps through the specified number of elements in the queue specified **within the current tick**.
- `queueStep <queue> [count=1] [radius] force`: Steps through the specified number of elements in the queue specified **stepping to the next tick if necessary**.

## To-do list

`-` = todo, `+` = done, `~` = in progress, `~~` = someone else is on it

- `-` Fix compat with lithium.
- `-` Allow the client to animate moving blocks 1 by 1, to show the movement when ticking BlockEntities.
- `-` Fix some chunk unloading stuff with player movement while frozen.
- `-` etc... (aka put it on cmp and have people report bugs)

- `~~` Fix compat with multimeter.
- `+` Fix an issue with tile ticks when one dimension is frozen and another isn't.
- `+` Fix fluidTick stepping saying "tile ticks".
- `~` Add QOL stuff to command syntax.
- `+` Capitalize dimension names.
- `+` Fix compat with g4mespeed.
- `+` Add radius flag to queueStep, because stepping through everything in the world before getting to what you want is annoying.
- `+` Give an indication when the count attempted to queueStep through is exhausted.
- `+` Add tile tick stepping and fluid tick stepping to queueStep.
- `+` Attempt to simplify all command feedback to occupy less screen space.
