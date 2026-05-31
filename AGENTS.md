# Splitscreen Support

Splitscreen Support is a client-side Minecraft mod that makes local
multiplayer easier by automatically positioning and restoring Minecraft
windows for split-screen play.

The mod allows multiple Minecraft instances to share a single display by
cycling through predefined window layouts and remembering window positions
between launches. It is intended to be used alongside launcher tools such as
PrismLauncher and controller mods such as Midnight Controls or Controlify. :contentReference[oaicite:0]{index=0}

The goal of the mod is to make local couch co-op Minecraft as simple and
frictionless as possible without modifying gameplay. :contentReference[oaicite:1]{index=1}

## Design Philosophy

When making changes, prioritize:

1. Ease of use.
2. Reliability across operating systems.
3. Predictable window positioning behavior.
4. Local multiplayer usability.
5. Minimal configuration.

The mod should remain lightweight and focused.

Avoid adding features unrelated to local multiplayer setup and usability.

Favor simple, intuitive workflows that reduce the number of steps required to
start a multiplayer session.

## Project Structure

This project supports multiple mod loaders.

- `common/` contains shared window management and configuration logic.
- `fabric/` contains Fabric-specific code.
- `neoforge/` contains NeoForge-specific code.

Whenever possible, shared functionality should live in `common/`.

Loader-specific code should remain isolated to the appropriate platform
module. :contentReference[oaicite:2]{index=2}

## Repository Scope

This repository contains the source code for Splitscreen Support.

Only inspect files tracked by git.

Ignore:

- `build/`
- `.gradle/`
- `run/`
- `logs/`
- generated resources
- IDE metadata
- temporary files
- crash reports

Do not spend time analyzing generated files or build outputs.

## Cost-Aware Development

Repository-wide scans are expensive and should be avoided.

Before exploring the repository:

- Prefer targeted analysis.
- Read only files likely to be relevant.
- Start from files explicitly mentioned in the task.
- Follow references outward only as needed.
- Do not read entire directory trees unless necessary.

When discovering files, prefer:

```bash
git ls-files