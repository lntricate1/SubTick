# SubTick

[![License](https://img.shields.io/github/license/Fallen-Breath/fabric-mod-template.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)

This mod uses [Fallen's fabric mod template](https://github.com/Fallen-Breath/fabric-mod-template).

A carpet extension that allows you to freeze and step to any specific tick phase, as well as step through tile ticks, fluid ticks, block events, entities, and block entities individually. **You need it on your client to see highlights for queueStep!**

**Temporarily all dimensions have independent tick step. Support for both non-independent tick step and dimension-specific tick rate is planned.**

## Carpet rules

This mod uses carpet rules for its configuration options. For how to use the text formatting, search for "`format(components, ...)`" in [Auxiliary.md](https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md). For calculating the color code for `subtickHighlightColor`, you can run for example `/script run 0x00FF00` and it'll output the integer value.

- `subtickDefaultPhase=blockTick`: The default tick phase to freeze at and step to, if it's not specified in the command.
- `subtickDefaultRange=32`: The default range within which to queueStep.
- `subtickHighlightColor=0x00FF00`: The color to send highlights in to clients when queueStepping. Does not support alpha/transparency.
- `subtickTextFormat=ig`: The format for command feedback text.
- `subtickNumberFormat=iy`: The format for command feedback numbers.
- `subtickPhaseFormat=it`: The format for command feedback phases.
- `subtickDimensionFormat=im`: The format for command feedback dimensions.
- `subtickErrorFormat=ir`: The format for command feedback errors.

## Commands

*[] represents an optional argument, and <> represents an obligatory argument. If an argument is written like `count=1`, that means `1` is the default value.*

- `tick freeze [phase=subtickDefaultPhase]`: Freezes/unfreezes right before `phase`.
- `tick step [count=1] [phase=subtickDefaultPhase]`: Steps `count` ticks, ending right before `phase`. Supports `tick step 0 [phase]` to step to a later phase in the same tick.
- `phaseStep [count=1]`: Steps the `count` phases forward, **stepping to the next tick if necessary**.
- `phaseStep [phase]`: Steps to `phase`, **within the current tick**.
- `phaseStep [phase] force`: Steps to the next `phase` **stepping to the next tick if necessary**.
- `queueStep <queue> [count=1] [range=subtickDefaultRange]`: Steps through `count` elements in `queue` in range `range`, **within the current tick**. Set `range` to `-1` for unlimited range.
- `queueStep <queue> [count=1] [range=subtickDefaultRange] force`: Steps through `count` elements in `queue` in range `range`, **stepping to the next tick if necessary**. Set `range` to `-1` for unlimited range.

### Special cases

Block events and block ticks have the option to use a different mode for stepping. Block events can step through whole block event depths, and block ticks can step through whole block tick priorities.

- `queueStep blockEvent [mode=index] [count=1] [range=subtickDefaultRange] [force]`
- `queueStep blockTick [mode=index] [count=1] [range=subtickDefaultRange] [force]`
