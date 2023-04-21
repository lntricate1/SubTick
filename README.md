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

- `-` Fix some chunk unloading stuff with player movement while frozen.
- `-` etc... (aka put it on cmp and have people report bugs)

- `~~` Fix compat with multimeter.
- `~` Add QOL stuff to command syntax.
