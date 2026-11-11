## Overview

This project is very much stil a WIP, 

The goal of this project it to simulate D&D 5.5e combat encounters with full action economy, spell casting, attacks, saving throws, damage calculation, and effects systems. It supports both manual player control and AI-driven monster behavior.

Currently it has support for a lot of this but only a few abilities/actions have been implemented so far

## Features

- **Action Economy**: Full support for Actions, Bonus Actions, and Reactions
- **Spell System**: Level-based spells with schools (Abjuration, Evocation, Necromancy, etc.)
- **Combat Mechanics**: Attack rolls, advantage/disadvantage, critical hits, saving throws
- **Damage System**: Multiple damage types (Slashing, Piercing, Bludgeoning, Elemental, Force, etc.)
- **Effects System**: Ongoing effects with hooks for various combat events
- **AI Control**: Behavior strategies for monster AI decision-making
- **CLI Interface**: Command-line interface for player input

## Project Structure
src/main/java/com/dndcombat/
│
├── ai/                              # AI and decision logic
│   ├── ActionDecision.java           # AI decision record
│   ├── ActionDecisionParser.java     # JSON parser for AI decisions
│   ├── ActionOption.java             # Available action options
│   ├── BehaviorStrategy.java          # AI strategy interface
│   ├── CombatPromptBuilder.java       # LLM prompt builder
│   ├── CombatSnapshot.java            # Combat state snapshot
│   ├── CombatSnapshotFactory.java     # Snapshot creation
│   ├── DecisionValidator.java         # AI decision validation
│   ├── PlayerControlledStrategy.java  # Manual control strategy
│   └── ResolvedActionDecision.java    # Validated AI decision
│
├── actions/                          # Combat actions
│   ├── Action.java                    # Base action class
│   ├── ActionType.java                # ACTION, BONUS_ACTION, REACTION
│   ├── TargetingRequirements.java     # Target validation rules
│   │
│   ├── attacks/                       # Attack actions
│   │   └── WeaponAttack.java          # Weapon attack implementation
│   │
│   └── spells/                        # Spell actions
│       ├── Cantrip.java               # Level 0 spells
│       ├── School.java                # Spell schools
│       ├── Spell.java                 # Base spell class
│       │
│       ├── levelOne/                  # Level 1 spells
│       │   ├── Fireball.java          # Placeholder
│       │   └── InflictWounds.java
│       │
│       └── cantrip/                   # Cantrips
│           ├── EldritchBlast.java
│           ├── Firebolt.java
│           └── RayOfFrost.java
│
├── core/                             # Core combat logic
│   ├── combat/                       # Combat management
│   │   ├── ActionResult.java          # Action results
│   │   ├── CombatLog.java             # Combat logging
│   │   └── CombatManager.java         # Main combat controller
│   │
│   ├── context/                      # Context objects for actions
│   │   ├── AttackContext.java         # Attack roll context
│   │   ├── DamageContext.java         # Damage calculation context
│   │   ├── EffectContext.java         # Base effect context
│   │   └── SaveContext.java           # Saving throw context
│   │
│   ├── creatures/                    # Creature types
│   │   ├── Creature.java              # Base creature class
│   │   ├── Monster.java               # AI-controlled enemies
│   │   └── PlayerCharacter.java       # Player characters
│   │
│   └── dice/
│       └── DiceRoller.java            # Dice rolling utilities
│
├── effects/                          # Ongoing effects
│   ├── EffectHook.java                # Event hooks
│   ├── OngoingEffect.java             # Base effect class
│   └── generic_effects/
│       └── BlurBuff.java
│
├── equipment/                        # Weapons and gear
│   ├── Weapon.java                    # Weapon definition
│   ├── WeaponMastery.java              # Mastery interface
│   ├── WeaponProperty.java             # Weapon properties
│   └── weapons/
│       └── Weapons.java                # Weapon registry
│
├── enums/                            # Enumerations
│   ├── AbilityType.java                # STR, DEX, CON, INT, WIS, CHA
│   ├── AttackType.java                 # MELEE/RANGED, WEAPON/SPELL
│   ├── CastingTime.java                # ACTION, BONUS_ACTION, REACTION
│   ├── CharacterClass.java             # Fighter, Rogue, Wizard, etc.
│   ├── CreatureType.java               # Humanoid, Beast, Dragon, etc.
│   ├── DamageType.java                 # Slashing, Piercing, Fire, etc.
│   └── Team.java                       # PARTY, ENEMIES, NEUTRAL
│
└── ui/                               # User interfaces
    ├── CLIInputHandler.java            # Command-line input
    └── InputHandler.java               # Input interface

Requirements
- Java 17+
- Maven 3.6+
- Jackson (for JSON serialization)

Building
mvn clean package

Running
java -cp target/classes com.dndcombat.Main

Combat Flow
1. Initiative: All combatants roll d20 + Dexterity modifier.
2. Turn Processing: Combatants act in initiative order.
3. Action Selection: Choose available actions (Attack, Cast Spell, etc.).
4. Targeting: Select valid targets based on action requirements.
5. Resolution: Roll attacks/saving throws and calculate damage.
6. Effects: Apply ongoing effects and hooks.
7. Repeat until combat ends.

Key Classes
Creature Types
- PlayerCharacter: Player-controlled characters with resource management (Rage, Ki, Spell Points).
- Monster: AI-controlled enemies with behavior strategies.

Actions
- WeaponAttack: Melee or ranged weapon attacks.
- Spell: Spells requiring saving throws or attack rolls.
- Cantrip: Level 0 spells (Eldritch Blast, Firebolt, Ray of Frost).

Context Objects
- AttackContext: Handles attack rolls with advantage/disadvantage.
- DamageContext: Manages damage calculation with resistances/immunities.
- SaveContext: Handles saving throw resolution.

Effects System
- OngoingEffect: Base class for buffs/debuffs.
- EffectHook: Event hooks (BEFORE_DAMAGE_DEALT, WHEN_BEING_ATTACKED, etc.).
- BlurBuff: Example effect that grants disadvantage on attacks.

AI Integration
The project supports LLM-based AI through:
- CombatPromptBuilder: Generates prompts for AI decision-making.
- SnapshotSerializer: Serializes combat state to JSON.
- DecisionValidator: Validates AI decisions against combat rules.