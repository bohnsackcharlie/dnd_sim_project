# D&D Combat Simulator - Software Architecture

## Overview
A turn-based D&D 5e combat simulator built in Java with extensible class structures for creatures, actions, effects, and combat management. Designed for CLI interaction initially, with architecture supporting future UI and AI agent integration.

Key architectural patterns:
- **Typed Context Hierarchy** — Phase-specific context objects (`AttackContext`, `SaveContext`, `DamageContext`) extend a base `EffectContext`, providing compile-time safety and eliminating stringly-typed data passing for common cases
- **Hook-Based Effect System with Priority** — Effects register for specific lifecycle hooks and fire in priority order, allowing deterministic resolution when multiple effects compete
- **Reaction Trigger Pattern** — Reactions are modeled as persistent trigger effects in a creature's `activeEffects` list. They listen for hooks, prompt the user, and create separate buff/debuff effects — no special reaction dispatch needed
- **Team-Based Faction System** — Creatures belong to a `Team` enum rather than relying on `instanceof` checks for hostility, supporting future scenarios like charm, PvP, or neutral NPCs

---

## Core Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│              (CLI Interface - expandable)                │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                   Combat Manager Layer                   │
│     (Turn management, initiative, combat flow)           │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                   Game Logic Layer                       │
│   (Creatures, Actions, Effects, Dice Rolling)            │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                          │
│         (Combat state, creature definitions)             │
└─────────────────────────────────────────────────────────┘
```

---

## Class Structure

### 1. Creature System

```java
// Team-based faction system (replaces instanceof checks for hostility)
enum Team {
    PARTY,
    ENEMIES,
    NEUTRAL;

    public boolean isHostileTo(Team other) {
        if (this == NEUTRAL || other == NEUTRAL) return false;
        return this != other;
    }
}

// Base class for all combatants
abstract class Creature {
    // Identity
    private String name;
    private Team team;

    // Core Stats
    private int currentHP;
    private int maxHP;
    private int armorClass;

    // Ability Scores
    private AbilityScores abilityScores;

    // Saving Throws (proficiencies stored here)
    private SavingThrows savingThrows;

    // Skills
    private Skills skills;

    // Combat State
    // IMPORTANT: activeEffects includes reaction triggers (e.g., ShieldReactionTrigger).
    // There is no separate availableReactions list — reactions live here and fire
    // through the same hook dispatch as everything else.
    private List<OngoingEffect> activeEffects;
    private OngoingEffect concentratingOn; // null if not concentrating
    private List<Action> availableActions;
    private List<Action> availableBonusActions;

    // Action Economy (reset each turn)
    private boolean actionUsed;
    private boolean bonusActionUsed;
    private boolean reactionUsed;
    private int movementRemaining;

    // Calculated Methods (compute on-the-fly, considering equipment and effects)
    public int getAttackBonus(Weapon weapon);
    public int getSpellSaveDC();
    public int getSavingThrowBonus(AbilityType abilityType);
    public int getInitiativeBonus();

    // Effect Management
    public void addEffect(OngoingEffect effect);
    public void removeEffect(OngoingEffect effect);

    // Trigger effects registered for a specific hook.
    // Effects fire in priority order (lower number = fires first).
    // This means modifying effects (Bane, Bless) fire before reaction triggers (Shield).
    public void triggerEffects(EffectHook hook, EffectContext context) {
        activeEffects.stream()
            .filter(e -> e.getRegisteredHooks().contains(hook))
            .sorted(Comparator.comparingInt(OngoingEffect::getPriority))
            .forEach(e -> e.apply(hook, context));
    }

    // Damage & Healing
    public void takeDamage(int amount);
    public void heal(int amount);

    // Concentration
    public boolean makeConcentrationSave(int damageAmount);
    public void breakConcentration();
    public boolean isConcentratingOn(OngoingEffect effect) {
        return concentratingOn == effect;
    }
    public void setConcentration(OngoingEffect effect) {
        if (concentratingOn != null) {
            breakConcentration();
        }
        concentratingOn = effect;
    }

    // Team helpers
    public boolean isHostileTo(Creature other) {
        return team.isHostileTo(other.getTeam());
    }
}

class PlayerCharacter extends Creature {
    private int level;
    private CharacterClass characterClass;
    private List<Feature> classFeatures;
    private List<Spell> knownSpells;
    private SpellSlots spellSlots;
    private Equipment equipment;

    // Resource tracking (ki points, rage uses, etc.)
    private Map<ResourceType, Integer> resources;
}

class Monster extends Creature {
    private double challengeRating;
    private CreatureType type; // Beast, Humanoid, Dragon, etc.
    private List<SpecialAbility> specialAbilities;

    // For future AI agent
    private BehaviorStrategy aiStrategy; // Interface for decision-making
}

// Supporting Classes
class AbilityScores {
    private int strength, dexterity, constitution;
    private int intelligence, wisdom, charisma;

    public int getModifier(AbilityType type);
}

class SavingThrows {
    private Set<AbilityType> proficiencies;

    public int getBonus(AbilityType type, AbilityScores scores);
}
```

### 2. Action System

**IMPORTANT CLARIFICATIONS:**
- The `CombatManager` creates a base `EffectContext` and passes it to `Action.execute()`
- Actions create **typed sub-contexts** (`AttackContext`, `SaveContext`, `DamageContext`) for each phase of resolution
- Effects cast the context to the expected type based on which hook they registered for (see Hook-to-Context Type Mapping in section 4)

```java
// Base action interface
abstract class Action {
    protected String name;
    protected String description;
    protected ActionType actionType; // ACTION, BONUS_ACTION, REACTION
    protected TargetingRequirements targetingReqs;

    // Core execution method - receives base context from CombatManager
    public abstract ActionResult execute(EffectContext context);

    // Validation
    public boolean canUse(Creature user, EffectContext context);
    public boolean isValidTarget(Creature target, Creature user);
}

// Weapon System - base class for all weapons.
// Standard weapons are simple instances; magic weapons with special effects
// subclass Weapon and override onHit().
class Weapon {
    private String name;
    private int damageDice;      // e.g., 8 for 1d8
    private int damageDieCount;  // e.g., 1 for 1d8
    private DamageType damageType;
    private WeaponProperty[] properties; // FINESSE, HEAVY, REACH, TWO_HANDED, VERSATILE, etc.
    private int magicBonus;      // +0, +1, +2, +3 for magic weapons
    private WeaponMastery mastery; // Optional weapon mastery effects

    public int rollDamage() {
        return DiceRoller.roll(damageDieCount, damageDice);
    }

    public boolean hasProperty(WeaponProperty prop) {
        return Arrays.asList(properties).contains(prop);
    }

    public int getMagicBonus() {
        return magicBonus;
    }

    public void setMagicBonus(int bonus) {
        this.magicBonus = bonus;
    }

    // On-hit callback — override in subclasses for magic weapon effects.
    // Called during WeaponAttack.execute() after base damage is rolled but
    // before BEFORE_DAMAGE hooks fire, so all components are visible to effects.
    // Default: no-op (normal weapons don't add anything).
    public void onHit(DamageContext damageCtx) {}
}

// Standard weapons as constants (no subclass needed)
class Weapons {
    public static final Weapon LONGSWORD = new Weapon(
        "Longsword", 1, 8, DamageType.SLASHING,
        new WeaponProperty[]{WeaponProperty.VERSATILE}
    );

    public static final Weapon GREATSWORD = new Weapon(
        "Greatsword", 2, 6, DamageType.SLASHING,
        new WeaponProperty[]{WeaponProperty.HEAVY, WeaponProperty.TWO_HANDED}
    );

    public static final Weapon SHORTBOW = new Weapon(
        "Shortbow", 1, 6, DamageType.PIERCING,
        new WeaponProperty[]{WeaponProperty.AMMUNITION, WeaponProperty.TWO_HANDED}
    );

    // Create magic weapons dynamically (simple +X weapons)
    public static Weapon createMagicLongsword(int bonus) {
        Weapon magic = new Weapon(
            "Longsword +" + bonus, 1, 8, DamageType.SLASHING,
            new WeaponProperty[]{WeaponProperty.VERSATILE}
        );
        magic.setMagicBonus(bonus);
        return magic;
    }
}

// Magic weapons with special on-hit effects subclass Weapon (not WeaponAttack).
// This keeps the weapon's behavior WITH the weapon, not with the attack action.
// A single WeaponAttack class handles all weapons — magic or mundane.
class FlameTongue extends Weapon {
    public FlameTongue(int damageDieCount, int damageDice, DamageType baseType,
                       WeaponProperty[] properties) {
        super("Flame Tongue", damageDieCount, damageDice, baseType, properties);
    }

    @Override
    public void onHit(DamageContext damageCtx) {
        // Add 2d6 fire as an additional component in the same damage instance.
        // Because it's in the same DamageContext, it counts as one damage event
        // for concentration saves (DC based on total slashing + fire).
        int fireDamage = DiceRoller.roll(2, 6);
        damageCtx.addComponent(DamageType.FIRE, fireDamage);
    }

    // Factory methods for common Flame Tongue variants
    public static FlameTongue greatsword() {
        return new FlameTongue(2, 6, DamageType.SLASHING,
            new WeaponProperty[]{WeaponProperty.HEAVY, WeaponProperty.TWO_HANDED});
    }

    public static FlameTongue longsword() {
        return new FlameTongue(1, 8, DamageType.SLASHING,
            new WeaponProperty[]{WeaponProperty.VERSATILE});
    }
}

// Weapon Mastery as Strategy Pattern
interface WeaponMastery {
    void applyOnAttack(AttackContext context);
    void applyOnHit(AttackContext context, Creature target);
}

class LongswordMastery implements WeaponMastery {
    @Override
    public void applyOnAttack(AttackContext context) {
        // Sap: prepare to reduce target speed on hit
    }

    @Override
    public void applyOnHit(AttackContext context, Creature target) {
        // Reduce target speed until end of their next turn
        target.addEffect(new SlowedEffect(1));
    }
}

class GreataxeMastery implements WeaponMastery {
    @Override
    public void applyOnAttack(AttackContext context) {
        // Cleave: on crit or kill, allow extra attack
    }

    @Override
    public void applyOnHit(AttackContext context, Creature target) {
        // Applied after hit
    }
}

// Single WeaponAttack class that works with ALL weapons — mundane and magic.
// Magic weapon on-hit effects are handled by the weapon itself via onHit().
class WeaponAttack extends Action {
    private Weapon weapon;

    public WeaponAttack(Weapon weapon) {
        super(weapon.getName() + " Attack", ActionType.ACTION);
        this.weapon = weapon;
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature attacker = context.getActionUser();
        Creature target = context.getPrimaryTarget();

        // === ATTACK PHASE — create typed AttackContext ===
        // The action knows it's a weapon attack; determine melee vs ranged from weapon properties.
        // The weapon itself stays local to this action — AttackContext is weapon-agnostic.
        AttackType type = weapon.hasProperty(WeaponProperty.AMMUNITION)
            ? AttackType.RANGED_WEAPON : AttackType.MELEE_WEAPON;
        AttackContext attackCtx = new AttackContext(context, type);

        // BEFORE_ATTACK_ROLL hooks
        // Both attacker and target modify the SAME AttackContext
        attacker.triggerEffects(EffectHook.BEFORE_ATTACK_ROLL, attackCtx);
        // RecklessAttack calls attackCtx.grantAdvantage()

        target.triggerEffects(EffectHook.WHEN_BEING_ATTACKED, attackCtx);
        // If target has RecklessAttack buff, also calls attackCtx.grantAdvantage()
        // If target has Dodge, calls attackCtx.grantDisadvantage()

        // Apply weapon mastery effects before roll
        if (weapon.getMastery() != null) {
            weapon.getMastery().applyOnAttack(attackCtx);
        }

        // === MAKE THE ATTACK ROLL ===
        // AttackContext handles advantage/disadvantage logic internally
        int attackRoll = attackCtx.rollAttack();
        int attackBonus = attacker.getAttackBonus(weapon) + weapon.getMagicBonus();
        int totalAttack = attackCtx.applyRollModifiers(attackRoll + attackBonus);

        attackCtx.setAttackRoll(attackRoll);
        attackCtx.setAttackTotal(totalAttack);
        attackCtx.setCritical(attackRoll == 20);

        // === CHECK FOR HIT ===
        if (totalAttack >= target.getArmorClass() || attackCtx.isCritical()) {
            // WHEN_HIT_BY_ATTACK hook — reaction triggers fire here
            // e.g., ShieldReactionTrigger checks conditions, prompts user,
            // creates ShieldBuff, and may set attackCtx.setAttackNegated(true)
            target.triggerEffects(EffectHook.WHEN_HIT_BY_ATTACK, attackCtx);

            // A reaction (like Shield) may have negated the attack
            if (attackCtx.isAttackNegated()) {
                return ActionResult.miss();
            }

            // === DAMAGE PHASE — create typed DamageContext ===
            int damageRoll = weapon.rollDamage();
            int damageBonus = attacker.getDamageBonus(weapon) + weapon.getMagicBonus();

            // Critical hit on natural 20 - double the dice
            if (attackCtx.isCritical()) {
                damageRoll += weapon.rollDamage();
            }

            DamageContext damageCtx = new DamageContext(
                context, damageRoll + damageBonus, weapon.getDamageType());

            // Weapon on-hit effects (e.g., FlameTongue adds 2d6 fire here).
            // This fires BEFORE hooks, so all damage components are visible
            // to effects like Rage resistance and Absorb Elements.
            weapon.onHit(damageCtx);

            // BEFORE_DAMAGE hooks — both sides modify the same DamageContext.
            // All components are present, so effects see the full damage picture.
            attacker.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);
            // Rage calls damageCtx.addModifier(+2) on the primary (weapon) component

            target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);
            // Rage calls damageCtx.addPhysicalResistance() — halves slashing but not fire
            // AbsorbElementsReactionTrigger could fire here too

            // Apply weapon mastery effects on hit
            if (weapon.getMastery() != null) {
                weapon.getMastery().applyOnHit(attackCtx, target);
            }

            // Single path for final damage — all components resolved together
            int totalDamage = damageCtx.calculateFinalDamage();
            target.takeDamage(totalDamage);

            return ActionResult.hit(totalDamage, damageCtx, attackCtx.isCritical());
        }

        return ActionResult.miss();
    }

}

// Spell System
abstract class Spell extends Action {
    protected int level;
    protected School school;
    protected boolean requiresConcentration;
    protected int duration; // in turns
    protected CastingTime castingTime;

    protected int getSpellSaveDC(Creature caster) {
        return 8 + caster.getProficiencyBonus() +
               caster.getAbilityScores().getModifier(caster.getSpellcastingAbility());
    }
}

class Fireball extends Spell {
    @Override
    public ActionResult execute(EffectContext context) {
        Creature caster = context.getActionUser();
        List<Creature> targets = context.getTargets();

        int saveDC = getSpellSaveDC(caster);
        int baseDamage = DiceRoller.roll(8, 6); // 8d6

        ActionResult result = new ActionResult();

        for (Creature target : targets) {
            // === SAVE PHASE — create typed SaveContext for each target ===
            SaveContext saveCtx = new SaveContext(
                context, AbilityType.DEXTERITY, saveDC);

            // BEFORE_SAVING_THROW hook
            target.triggerEffects(EffectHook.BEFORE_SAVING_THROW, saveCtx);
            // Bane subtracts 1d4 via saveCtx.addModifier(-1d4)

            int saveRoll = DiceRoller.d20();
            int saveBonus = target.getSavingThrowBonus(AbilityType.DEXTERITY);
            int totalSave = saveCtx.applyModifiers(saveRoll + saveBonus);

            saveCtx.setSaveRoll(saveRoll);
            saveCtx.setSaveTotal(totalSave);
            saveCtx.setSucceeded(totalSave >= saveDC);

            // === CHECK FOR REACTIONS TO SAVE (Silvery Barbs) ===
            // Check all OTHER hostile creatures for reactions
            for (Creature other : context.getCombatManager().getAllCombatants()) {
                if (other == target || !other.hasReactionAvailable()) continue;
                if (!other.isHostileTo(target)) continue;

                other.triggerEffects(EffectHook.AFTER_ENEMY_SAVING_THROW, saveCtx);

                // SilveryBarbsReactionTrigger sets forceReroll if it fires
                if (saveCtx.isForceReroll()) {
                    saveRoll = DiceRoller.d20();
                    totalSave = saveCtx.applyModifiers(saveRoll + saveBonus);
                    saveCtx.setSaveRoll(saveRoll);
                    saveCtx.setSaveTotal(totalSave);
                    saveCtx.setSucceeded(totalSave >= saveDC);
                    saveCtx.setForceReroll(false); // consumed
                    break;
                }
            }

            // === DAMAGE PHASE — create typed DamageContext for each target ===
            int damage = saveCtx.isSucceeded() ? baseDamage / 2 : baseDamage;

            DamageContext damageCtx = new DamageContext(
                context, damage, DamageType.FIRE);

            target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);
            // AbsorbElementsReactionTrigger could fire here, calling damageCtx.addResistance(FIRE)

            int totalDamage = damageCtx.calculateFinalDamage();
            target.takeDamage(totalDamage);
            result.addTargetResult(target, totalDamage, saveCtx.isSucceeded());
        }

        return result;
    }
}

class Bane extends Spell {
    @Override
    public ActionResult execute(EffectContext context) {
        Creature user = context.getActionUser();
        List<Creature> targets = context.getTargets();

        int saveDC = getSpellSaveDC(user);
        ActionResult result = new ActionResult();

        for (Creature target : targets) {
            // Create typed SaveContext for each target's saving throw
            SaveContext saveCtx = new SaveContext(
                context, AbilityType.CHARISMA, saveDC);

            target.triggerEffects(EffectHook.BEFORE_SAVING_THROW, saveCtx);

            int saveRoll = DiceRoller.d20();
            int saveBonus = target.getSavingThrowBonus(AbilityType.CHARISMA);
            int totalSave = saveCtx.applyModifiers(saveRoll + saveBonus);

            if (totalSave < saveDC) {
                // Failed save - apply Bane debuff
                BaneDebuff debuff = new BaneDebuff(user, 10, requiresConcentration);
                target.addEffect(debuff);

                if (requiresConcentration) {
                    user.setConcentration(debuff);
                }

                result.addTargetResult(target, 0, false);
            } else {
                result.addTargetResult(target, 0, true);
            }
        }

        return result;
    }
}

// Class Feature Actions
class Rage extends Action {
    private static final int DURATION = 100; // turns

    @Override
    public ActionResult execute(EffectContext context) {
        Creature user = context.getActionUser();

        RageBuff buff = new RageBuff(DURATION);
        user.addEffect(buff);

        // Consume rage use
        user.consumeResource(ResourceType.RAGE_USES, 1);

        return ActionResult.success();
    }
}
```

### 3. Ongoing Effect System

```java
// Attack classification — used by AttackContext and effects that care about attack type
enum AttackType {
    MELEE_WEAPON,
    RANGED_WEAPON,
    MELEE_SPELL,     // e.g., Shocking Grasp, Inflict Wounds
    RANGED_SPELL;    // e.g., Firebolt, Eldritch Blast

    public boolean isMelee() {
        return this == MELEE_WEAPON || this == MELEE_SPELL;
    }

    public boolean isRanged() {
        return this == RANGED_WEAPON || this == RANGED_SPELL;
    }

    public boolean isWeaponAttack() {
        return this == MELEE_WEAPON || this == RANGED_WEAPON;
    }

    public boolean isSpellAttack() {
        return this == MELEE_SPELL || this == RANGED_SPELL;
    }
}

// Effect Hook Points - when effects can trigger
enum EffectHook {
    START_OF_TURN,
    END_OF_TURN,

    // Attack-related hooks (receive AttackContext)
    BEFORE_ATTACK_ROLL,      // When YOU make an attack roll
    AFTER_ATTACK_ROLL,
    WHEN_BEING_ATTACKED,     // When someone attacks YOU (before roll)
    WHEN_HIT_BY_ATTACK,      // When an attack hits YOU (after roll, before damage)

    // Damage-related hooks (receive DamageContext)
    BEFORE_DAMAGE_DEALT,     // When YOU deal damage
    AFTER_DAMAGE_DEALT,
    BEFORE_DAMAGE_TAKEN,     // When YOU take damage
    AFTER_DAMAGE_TAKEN,

    // Saving throw hooks (receive SaveContext)
    BEFORE_SAVING_THROW,     // When YOU make a saving throw
    AFTER_SAVING_THROW,
    AFTER_ENEMY_SAVING_THROW,  // When an ENEMY makes a save (for Silvery Barbs)
    WHEN_FORCING_SAVE,       // When YOU force someone to make a save

    // Spell-related hooks (receive base EffectContext)
    WHEN_CASTING_SPELL,

    // Other hooks
    AFTER_ENEMY_ATTACK_ROLL  // When an ENEMY makes an attack (for Silvery Barbs on attacks)
}

abstract class OngoingEffect {
    protected String name;
    protected Creature source; // Who created this effect
    protected Creature target; // Who has this effect
    protected int remainingDuration; // -1 for permanent (e.g., reaction triggers)
    protected boolean requiresConcentration;

    // Priority determines firing order within a hook (lower = fires first).
    // This matters when effects interact — e.g., Bane's penalty should apply
    // to the roll before a reaction trigger decides whether to fire.
    protected int priority = PRIORITY_NORMAL;

    // Priority constants for common categories
    public static final int PRIORITY_EARLY = 25;       // Setup effects (Shield AC buff)
    public static final int PRIORITY_NORMAL = 100;      // Standard buffs/debuffs (Bane, Rage)
    public static final int PRIORITY_LATE = 175;        // Effects that read final state
    public static final int PRIORITY_REACTION = 200;    // Reaction triggers (fire last, after
                                                        // all modifiers are applied)

    // Hook Registration - subclasses override to register for specific hooks
    public Set<EffectHook> getRegisteredHooks() {
        return new HashSet<>();
    }

    // Called when a registered hook triggers.
    // The EffectContext will be a typed subclass (AttackContext, SaveContext, etc.)
    // depending on which hook is firing — see Hook-to-Context Type Mapping.
    public abstract void apply(EffectHook hook, EffectContext context);

    // Check if effect should end
    public boolean shouldEnd(EffectContext context);

    // Called when effect ends
    public void onEffectEnd();

    // Tick down duration
    public void decrementDuration();

    public int getPriority() { return priority; }
}

class BaneDebuff extends OngoingEffect {

    public BaneDebuff(Creature source, int duration, boolean concentration) {
        this.source = source;
        this.remainingDuration = duration;
        this.requiresConcentration = concentration;
        this.priority = PRIORITY_NORMAL;
    }

    @Override
    public Set<EffectHook> getRegisteredHooks() {
        return Set.of(
            EffectHook.BEFORE_ATTACK_ROLL,
            EffectHook.BEFORE_SAVING_THROW
        );
    }

    @Override
    public void apply(EffectHook hook, EffectContext context) {
        switch (hook) {
            case BEFORE_ATTACK_ROLL:
                // Cast to typed context — safe because BEFORE_ATTACK_ROLL
                // is always called with an AttackContext
                AttackContext attackCtx = (AttackContext) context;
                attackCtx.addRollModifier(-DiceRoller.d4());
                break;

            case BEFORE_SAVING_THROW:
                SaveContext saveCtx = (SaveContext) context;
                saveCtx.addModifier(-DiceRoller.d4());
                break;
        }
    }

    @Override
    public boolean shouldEnd(EffectContext context) {
        // Ends when duration expires or concentration broken
        return remainingDuration <= 0 || !source.isConcentratingOn(this);
    }
}

class RageBuff extends OngoingEffect {
    private boolean attackedThisTurn = false;

    public RageBuff(int duration) {
        this.remainingDuration = duration;
        this.priority = PRIORITY_NORMAL;
    }

    @Override
    public Set<EffectHook> getRegisteredHooks() {
        return Set.of(
            EffectHook.BEFORE_DAMAGE_DEALT,
            EffectHook.BEFORE_DAMAGE_TAKEN,
            EffectHook.WHEN_CASTING_SPELL,
            EffectHook.START_OF_TURN,
            EffectHook.END_OF_TURN
        );
    }

    @Override
    public void apply(EffectHook hook, EffectContext context) {
        switch (hook) {
            case BEFORE_DAMAGE_DEALT:
                // +2 to melee weapon damage
                DamageContext dmgDealtCtx = (DamageContext) context;
                if (context.getAction() instanceof WeaponAttack) {
                    WeaponAttack attack = (WeaponAttack) context.getAction();
                    if (attack.isMelee()) {
                        dmgDealtCtx.addModifier(+2);
                        attackedThisTurn = true;
                    }
                }
                break;

            case BEFORE_DAMAGE_TAKEN:
                // Resistance to bludgeoning, piercing, and slashing damage.
                // Per-type resistance means only physical components are halved —
                // fire damage from a Flame Tongue still hits at full.
                DamageContext dmgTakenCtx = (DamageContext) context;
                dmgTakenCtx.addPhysicalResistance();
                break;

            case WHEN_CASTING_SPELL:
                // Can't cast spells while raging — use contextData escape hatch
                // since this is an unusual action-cancellation case
                context.setContextData("cancelAction", true);
                context.setContextData("cancelReason", "Cannot cast spells while raging");
                break;

            case START_OF_TURN:
                attackedThisTurn = false;
                break;

            case END_OF_TURN:
                // Rage ends if didn't attack this turn
                if (!attackedThisTurn) {
                    target.removeEffect(this);
                }
                break;
        }
    }
}
```

#### Reaction Trigger Pattern

Reactions are modeled as two separate objects:
1. **Reaction Trigger** — A persistent `OngoingEffect` on the creature that listens for the relevant hook, checks if the reaction can be used (resources, reaction not spent), prompts the user, and if accepted, creates the buff/debuff and consumes resources.
2. **Reaction Buff** — A standard `OngoingEffect` representing the mechanical benefit (e.g., +5 AC from Shield).

This keeps reactions in the **same hook dispatch system** as everything else. No special reaction-checking code is needed — the trigger fires naturally when the right hook occurs. The `PRIORITY_REACTION` ensures triggers fire *after* all modifying effects have been applied, so the creature (or AI) can make an informed decision about whether to use the reaction.

When a creature learns or prepares a reaction-capable spell/ability, its corresponding trigger is added to `activeEffects` with `remainingDuration = -1` (permanent). The trigger handles its own prerequisite checks on each invocation.

```java
// === Shield Spell: Trigger + Buff ===

// Trigger: lives permanently in creature's activeEffects, fires on WHEN_HIT_BY_ATTACK
class ShieldReactionTrigger extends OngoingEffect {

    public ShieldReactionTrigger(Creature owner) {
        this.name = "Shield (Reaction)";
        this.target = owner;  // the creature who knows this spell
        this.remainingDuration = -1;  // permanent — always available
        this.priority = PRIORITY_REACTION;  // fires after Bane/Bless modifiers
    }

    @Override
    public Set<EffectHook> getRegisteredHooks() {
        return Set.of(EffectHook.WHEN_HIT_BY_ATTACK);
    }

    @Override
    public void apply(EffectHook hook, EffectContext context) {
        AttackContext attackCtx = (AttackContext) context;

        // Check prerequisites — bail silently if can't use
        if (target.isReactionUsed()) return;
        if (!target.hasSpellSlot(1)) return;

        // Prompt user (or AI) for reaction decision.
        // At this point, all modifiers are already applied (due to PRIORITY_REACTION),
        // so the player/AI has full information about the attack.
        if (!context.getCombatManager().promptReaction(target, this, attackCtx)) {
            return;  // declined
        }

        // === Reaction accepted ===

        // Consume resources
        target.consumeSpellSlot(1);
        target.setReactionUsed(true);

        // Create the Shield buff (+5 AC until start of caster's next turn)
        ShieldBuff buff = new ShieldBuff(target);
        target.addEffect(buff);

        // Check if the +5 AC now negates the hit that triggered this
        if (attackCtx.getAttackTotal() < target.getArmorClass()) {
            attackCtx.setAttackNegated(true);
        }
    }

    @Override
    public boolean shouldEnd(EffectContext context) {
        return false;  // never expires — always available as long as creature knows Shield
    }
}

// Buff: the actual mechanical effect of Shield (separate from the trigger)
class ShieldBuff extends OngoingEffect {
    private static final int AC_BONUS = 5;

    public ShieldBuff(Creature target) {
        this.name = "Shield";
        this.target = target;
        this.remainingDuration = 1;  // until start of caster's next turn
        this.priority = PRIORITY_EARLY;  // AC bonus applies early in hook resolution
    }

    @Override
    public Set<EffectHook> getRegisteredHooks() {
        // Boosts AC for any subsequent attacks this round
        // (the initial triggering attack was already handled by the trigger)
        return Set.of(EffectHook.WHEN_BEING_ATTACKED);
    }

    @Override
    public void apply(EffectHook hook, EffectContext context) {
        // Add +5 AC for any attack checks against this creature
        target.addTemporaryACBonus(AC_BONUS);
    }

    @Override
    public boolean shouldEnd(EffectContext context) {
        return remainingDuration <= 0;
    }
}

// === Absorb Elements: Same pattern, different hook ===

class AbsorbElementsReactionTrigger extends OngoingEffect {

    public AbsorbElementsReactionTrigger(Creature owner) {
        this.name = "Absorb Elements (Reaction)";
        this.target = owner;
        this.remainingDuration = -1;
        this.priority = PRIORITY_REACTION;
    }

    @Override
    public Set<EffectHook> getRegisteredHooks() {
        // Fires on incoming elemental damage, not on attack hit
        return Set.of(EffectHook.BEFORE_DAMAGE_TAKEN);
    }

    @Override
    public void apply(EffectHook hook, EffectContext context) {
        DamageContext damageCtx = (DamageContext) context;

        // Prerequisites
        if (target.isReactionUsed()) return;
        if (!target.hasSpellSlot(1)) return;
        if (!damageCtx.includesElementalDamage()) return;

        if (!context.getCombatManager().promptReaction(target, this, damageCtx)) {
            return;
        }

        // Consume resources
        target.consumeSpellSlot(1);
        target.setReactionUsed(true);

        // Add resistance to each elemental damage type present in this instance
        for (DamageComponent comp : damageCtx.getComponents()) {
            if (comp.getType().isElemental()) {
                damageCtx.addResistance(comp.getType());
            }
        }

        // Add bonus damage to next melee attack (uses the triggering element type)
        target.addEffect(new AbsorbElementsBuff(target, damageCtx.getPrimaryDamageType()));
    }

    @Override
    public boolean shouldEnd(EffectContext context) {
        return false;
    }
}
```

### 4. Context Hierarchy

The base `EffectContext` holds shared fields. Typed sub-contexts add phase-specific state with real fields, eliminating most uses of the stringly-typed `contextData` map. The `contextData` map remains as an escape hatch for truly unusual effects (e.g., Rage's spell-cancellation).

**Key insight**: The context still represents THE ACTION/EVENT, not the actor. Multiple creatures modify the same context as it flows through resolution. The typed hierarchy just makes the available fields explicit and compile-time safe.

```java
// Base context — shared fields for all effect triggers
class EffectContext {
    private CombatManager combatManager;
    private Action action;           // null if not during an action (e.g., start of turn)
    private Creature actionUser;     // Who is PERFORMING the action (attacker, caster)
    private List<Creature> targets;  // Who is being targeted
    private Map<String, Object> contextData;  // escape hatch for unusual effects

    // Full constructor for action execution
    public EffectContext(CombatManager combatManager, Action action,
                        Creature actionUser, List<Creature> targets) {
        this.combatManager = combatManager;
        this.action = action;
        this.actionUser = actionUser;
        this.targets = targets;
        this.contextData = new HashMap<>();
    }

    // Simpler constructor for non-action effects (like start of turn)
    public EffectContext(CombatManager combatManager, Creature actionUser) {
        this(combatManager, null, actionUser, new ArrayList<>());
    }

    // Getters
    public CombatManager getCombatManager() { return combatManager; }
    public Action getAction() { return action; }
    public Creature getActionUser() { return actionUser; }
    public List<Creature> getTargets() { return targets; }
    public Creature getPrimaryTarget() {
        return targets.isEmpty() ? null : targets.get(0);
    }

    // Context data escape hatch — use typed context fields instead when possible
    public void setContextData(String key, Object value) {
        contextData.put(key, value);
    }

    public Object getContextData(String key) {
        return contextData.get(key);
    }
}

// Attack phase context — used for all attack-related hooks.
// Deliberately weapon-agnostic: the Action (WeaponAttack, Firebolt, etc.) knows
// what's generating the attack and computes bonuses. The context just tracks
// the shared attack-roll state that effects interact with.
class AttackContext extends EffectContext {
    private AttackType attackType;       // melee/ranged, weapon/spell
    private int attackRoll;
    private int attackTotal;
    private boolean critical;
    private boolean attackNegated;      // set by Shield, etc.

    // Advantage/disadvantage tracking
    private boolean hasAdvantage;
    private boolean hasDisadvantage;

    // Flat roll modifiers (Bane -1d4, Bless +1d4, etc.)
    private int rollModifier;

    // Created from parent base context, inheriting shared fields
    public AttackContext(EffectContext parent, AttackType attackType) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.attackType = attackType;
    }

    // Advantage/Disadvantage
    public void grantAdvantage() { hasAdvantage = true; }
    public void grantDisadvantage() { hasDisadvantage = true; }

    public boolean shouldRollWithAdvantage() {
        // If both advantage and disadvantage, they cancel out (5e rule)
        if (hasAdvantage && hasDisadvantage) return false;
        return hasAdvantage;
    }

    public boolean shouldRollWithDisadvantage() {
        if (hasAdvantage && hasDisadvantage) return false;
        return hasDisadvantage;
    }

    // Roll the attack die (handles advantage/disadvantage internally)
    public int rollAttack() {
        if (shouldRollWithAdvantage()) {
            return Math.max(DiceRoller.d20(), DiceRoller.d20());
        } else if (shouldRollWithDisadvantage()) {
            return Math.min(DiceRoller.d20(), DiceRoller.d20());
        }
        return DiceRoller.d20();
    }

    // Flat roll modifiers
    public void addRollModifier(int mod) { rollModifier += mod; }

    public int applyRollModifiers(int baseRoll) {
        return baseRoll + rollModifier;
    }

    // Getters/setters
    public AttackType getAttackType() { return attackType; }
    public boolean isMelee() { return attackType.isMelee(); }
    public boolean isWeaponAttack() { return attackType.isWeaponAttack(); }
    public boolean isSpellAttack() { return attackType.isSpellAttack(); }
    public int getAttackRoll() { return attackRoll; }
    public void setAttackRoll(int roll) { this.attackRoll = roll; }
    public int getAttackTotal() { return attackTotal; }
    public void setAttackTotal(int total) { this.attackTotal = total; }
    public boolean isCritical() { return critical; }
    public void setCritical(boolean crit) { this.critical = crit; }
    public boolean isAttackNegated() { return attackNegated; }
    public void setAttackNegated(boolean negated) { this.attackNegated = negated; }
}

// Saving throw context — used for all save-related hooks
class SaveContext extends EffectContext {
    private AbilityType saveAbility;
    private int saveDC;
    private int saveRoll;
    private int saveTotal;
    private boolean succeeded;
    private boolean forceReroll;     // set by Silvery Barbs

    // Flat roll modifiers (Bane -1d4, Bless +1d4, etc.)
    private int rollModifier;

    public SaveContext(EffectContext parent, AbilityType ability, int dc) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.saveAbility = ability;
        this.saveDC = dc;
    }

    public void addModifier(int mod) { rollModifier += mod; }

    public int applyModifiers(int baseRoll) {
        return baseRoll + rollModifier;
    }

    // Getters/setters
    public AbilityType getSaveAbility() { return saveAbility; }
    public int getSaveDC() { return saveDC; }
    public int getSaveRoll() { return saveRoll; }
    public void setSaveRoll(int roll) { this.saveRoll = roll; }
    public int getSaveTotal() { return saveTotal; }
    public void setSaveTotal(int total) { this.saveTotal = total; }
    public boolean isSucceeded() { return succeeded; }
    public void setSucceeded(boolean success) { this.succeeded = success; }
    public boolean isForceReroll() { return forceReroll; }
    public void setForceReroll(boolean force) { this.forceReroll = force; }
}

// A single damage component — one damage type and amount within a damage instance
class DamageComponent {
    private DamageType type;
    private int amount;

    public DamageComponent(DamageType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public DamageType getType() { return type; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public void addAmount(int mod) { this.amount += mod; }
}

// Damage context — represents ONE INSTANCE of damage, which may include
// multiple damage types (e.g., Flame Tongue: slashing + fire).
//
// KEY DESIGN RULE: One attack = one DamageContext = one concentration save.
// The DC is based on the TOTAL damage across all components.
// Resistance/immunity/vulnerability are applied per damage type, so a raging
// barbarian hit by a Flame Tongue resists the slashing but takes full fire.
class DamageContext extends EffectContext {
    private List<DamageComponent> components;

    // Per-type resistance, immunity, vulnerability
    private Set<DamageType> resistedTypes = new HashSet<>();
    private Set<DamageType> immuneTypes = new HashSet<>();
    private Set<DamageType> vulnerableTypes = new HashSet<>();

    // Simple constructor for single-type damage (most common case)
    public DamageContext(EffectContext parent, int damage, DamageType type) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.components = new ArrayList<>();
        this.components.add(new DamageComponent(type, damage));
    }

    // Multi-type constructor for attacks with mixed damage
    public DamageContext(EffectContext parent, List<DamageComponent> components) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.components = new ArrayList<>(components);
    }

    // --- Component manipulation ---

    // Add another damage component (e.g., bonus fire damage from Flame Tongue)
    public void addComponent(DamageType type, int amount) {
        components.add(new DamageComponent(type, amount));
    }

    // Add a flat modifier to the PRIMARY (first) component.
    // This is what Rage +2, Dueling +2, etc. use — they modify the weapon damage.
    public void addModifier(int mod) {
        if (!components.isEmpty()) {
            components.get(0).addAmount(mod);
        }
    }

    // Add a modifier to a specific damage type
    public void addModifierToType(DamageType type, int mod) {
        for (DamageComponent comp : components) {
            if (comp.getType() == type) {
                comp.addAmount(mod);
                return;
            }
        }
    }

    // --- Per-type resistance/immunity/vulnerability ---

    public void addResistance(DamageType type) { resistedTypes.add(type); }
    public void addImmunity(DamageType type) { immuneTypes.add(type); }
    public void addVulnerability(DamageType type) { vulnerableTypes.add(type); }

    // Convenience: add resistance to all physical damage types
    public void addPhysicalResistance() {
        resistedTypes.add(DamageType.BLUDGEONING);
        resistedTypes.add(DamageType.PIERCING);
        resistedTypes.add(DamageType.SLASHING);
    }

    // --- Damage calculation ---

    // Single, deterministic damage calculation path.
    // Each component is processed independently against its type's resistances.
    // Order per component: base amount → vulnerability (×2) → resistance (÷2)
    // Then sum all components and floor at 0.
    //
    // This means a Flame Tongue hitting a raging barbarian:
    //   slashing 14 → halved to 7 (Rage resistance)
    //   fire 7 → full 7 (no fire resistance)
    //   total = 14 → one concentration save at DC max(10, 14/2) = DC 10
    public int calculateFinalDamage() {
        int total = 0;
        for (DamageComponent comp : components) {
            if (immuneTypes.contains(comp.getType())) continue;
            int dmg = comp.getAmount();
            if (vulnerableTypes.contains(comp.getType())) dmg *= 2;
            if (resistedTypes.contains(comp.getType())) dmg /= 2;
            total += dmg;
        }
        return Math.max(0, total);
    }

    // --- Query methods ---

    // Get all components (for logging, UI display)
    public List<DamageComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    // Get the primary (first) damage type — convenience for simple single-type cases
    public DamageType getPrimaryDamageType() {
        return components.isEmpty() ? null : components.get(0).getType();
    }

    // Check if this damage instance includes a specific type
    public boolean includesDamageType(DamageType type) {
        return components.stream().anyMatch(c -> c.getType() == type);
    }

    // Check if ANY component is a physical damage type
    public boolean includesPhysicalDamage() {
        return components.stream().anyMatch(c -> c.getType().isPhysical());
    }

    // Check if ANY component is an elemental damage type
    public boolean includesElementalDamage() {
        return components.stream().anyMatch(c -> c.getType().isElemental());
    }
}
```

**Hook-to-Context Type Mapping:**

Each hook is always called with a specific context type. Effects can safely cast to the expected type.

| Hook | Context Type | Notes |
|------|-------------|-------|
| `START_OF_TURN` | `EffectContext` | No action in progress |
| `END_OF_TURN` | `EffectContext` | No action in progress |
| `BEFORE_ATTACK_ROLL` | `AttackContext` | Modify advantage, roll modifiers |
| `AFTER_ATTACK_ROLL` | `AttackContext` | Read results |
| `WHEN_BEING_ATTACKED` | `AttackContext` | Target modifies advantage, roll modifiers |
| `WHEN_HIT_BY_ATTACK` | `AttackContext` | Reaction triggers (Shield) can negate |
| `BEFORE_DAMAGE_DEALT` | `DamageContext` | Attacker modifies damage (all components present) |
| `AFTER_DAMAGE_DEALT` | `DamageContext` | Read final damage (all components resolved) |
| `BEFORE_DAMAGE_TAKEN` | `DamageContext` | Target adds per-type resistance, reactions (Absorb Elements) |
| `AFTER_DAMAGE_TAKEN` | `DamageContext` | Read final damage taken (one instance, one conc. save) |
| `BEFORE_SAVING_THROW` | `SaveContext` | Modify save roll (Bane, Bless) |
| `AFTER_SAVING_THROW` | `SaveContext` | Read results |
| `AFTER_ENEMY_SAVING_THROW` | `SaveContext` | Reactions (Silvery Barbs) force reroll |
| `WHEN_FORCING_SAVE` | `SaveContext` | Caster-side save modifications |
| `WHEN_CASTING_SPELL` | `EffectContext` | Base context (Rage cancel uses contextData) |
| `AFTER_ENEMY_ATTACK_ROLL` | `AttackContext` | Silvery Barbs on attacks |

### 5. Combat Manager

```java
class CombatManager {
    private List<Creature> combatants;
    private Queue<Creature> initiativeOrder;
    private int currentRound;
    private Creature currentTurn;

    private CombatLog log;
    private InputHandler inputHandler; // For CLI or future UI

    public void startCombat() {
        // Roll initiative for all combatants
        rollInitiative();
        currentRound = 1;

        while (!combatEnded()) {
            processTurn(initiativeOrder.poll());
        }
    }

    private void processTurn(Creature creature) {
        currentTurn = creature;
        log.logTurnStart(creature);

        // Reset action economy
        creature.resetActions();

        // Trigger start-of-turn effects
        // Note: using simpler constructor since no action involved
        creature.triggerEffects(EffectHook.START_OF_TURN,
            new EffectContext(this, creature));

        // Check if creature can act (stunned, unconscious, etc.)
        if (!creature.canAct()) {
            processTurnEnd(creature);
            return;
        }

        // Process action phase
        processActionPhase(creature);

        // Process bonus action phase
        processBonusActionPhase(creature);

        // Process movement
        processMovement(creature);

        // End turn
        processTurnEnd(creature);
    }

    private void processActionPhase(Creature creature) {
        // Use Team to determine input source instead of instanceof
        if (creature.getTeam() == Team.PARTY) {
            // Get player input
            Action chosen = inputHandler.chooseAction(
                creature.getAvailableActions(), creature);

            if (chosen != null) {
                List<Creature> targets = inputHandler.chooseTargets(
                    chosen, combatants, creature);
                executeAction(creature, chosen, targets);
            }
        } else {
            // AI decision — Monster has a BehaviorStrategy
            Monster monster = (Monster) creature;
            Action chosen = monster.getAiStrategy()
                .chooseAction(creature, combatants);
            List<Creature> targets = monster.getAiStrategy()
                .chooseTargets(chosen, combatants, creature);

            if (chosen != null && !targets.isEmpty()) {
                executeAction(creature, chosen, targets);
            }
        }
    }

    private void executeAction(Creature user, Action action, List<Creature> targets) {
        // Create BASE context — action creates typed sub-contexts internally
        EffectContext context = new EffectContext(this, action, user, targets);

        // Execute the action — it creates AttackContext, SaveContext, DamageContext
        // as needed during its resolution phases
        ActionResult result = action.execute(context);

        // Log result
        log.logAction(user, action, targets, result);

        // Mark action as used
        if (action.getActionType() == ActionType.ACTION) {
            user.setActionUsed(true);
        }

        // Check for concentration breaks
        for (Creature target : targets) {
            if (result.dealtDamage(target)) {
                checkConcentration(target, result.getDamage(target));
            }
        }
    }

    private void processTurnEnd(Creature creature) {
        // Trigger end-of-turn effects
        creature.triggerEffects(EffectHook.END_OF_TURN,
            new EffectContext(this, creature));

        // Decrement effect durations
        creature.decrementEffectDurations();

        // Remove expired effects
        creature.removeExpiredEffects();

        log.logTurnEnd(creature);

        // Add back to initiative queue
        initiativeOrder.offer(creature);

        // Check if round ended
        if (initiativeOrder.peek() == combatants.get(0)) {
            currentRound++;
            log.logRoundEnd(currentRound - 1);
        }
    }

    // Called by reaction triggers to prompt the creature's controller
    public boolean promptReaction(Creature creature, OngoingEffect trigger,
                                  EffectContext context) {
        if (creature.getTeam() == Team.PARTY) {
            return inputHandler.promptYesNo(
                String.format("Use %s? (Reaction)", trigger.getName())
            );
        } else {
            return ((Monster) creature).getAiStrategy()
                .shouldUseReaction(trigger, context);
        }
    }

    private boolean combatEnded() {
        // Use Team instead of instanceof
        long aliveParty = combatants.stream()
            .filter(c -> c.getTeam() == Team.PARTY && c.isAlive())
            .count();
        long aliveEnemies = combatants.stream()
            .filter(c -> c.getTeam() == Team.ENEMIES && c.isAlive())
            .count();

        return aliveParty == 0 || aliveEnemies == 0;
    }
}
```

### 6. Supporting Systems

```java
class DiceRoller {
    private static Random random = new Random();

    public static int d20() {
        return random.nextInt(20) + 1;
    }

    public static int d4() {
        return random.nextInt(4) + 1;
    }

    public static int roll(int count, int sides) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1;
        }
        return total;
    }

    // For future: inject different implementations for testing
    public static void setRandom(Random r) {
        random = r;
    }
}

class CombatLog {
    private List<String> logEntries;

    public void logTurnStart(Creature creature) {
        log(String.format("=== %s's Turn ===", creature.getName()));
    }

    public void logAction(Creature user, Action action,
                         List<Creature> targets, ActionResult result) {
        log(String.format("%s uses %s", user.getName(), action.getName()));
        // Log detailed results
    }

    public void logDamage(Creature target, int damage, DamageType type) {
        log(String.format("%s takes %d %s damage",
            target.getName(), damage, type));
    }

    private void log(String message) {
        logEntries.add(message);
        System.out.println(message); // For CLI
    }
}

// Interface for future AI agent
interface BehaviorStrategy {
    Action chooseAction(Creature creature, List<Creature> allCombatants);
    List<Creature> chooseTargets(Action action, List<Creature> allCombatants,
                                  Creature user);
    boolean shouldUseReaction(OngoingEffect trigger, EffectContext context);
}

class SimpleBehaviorStrategy implements BehaviorStrategy {
    // Basic AI: attack nearest enemy with highest HP
    @Override
    public Action chooseAction(Creature creature, List<Creature> allCombatants) {
        // Simple: just pick first available attack
        return creature.getAvailableActions().stream()
            .filter(a -> a instanceof WeaponAttack)
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<Creature> chooseTargets(Action action,
                                         List<Creature> allCombatants,
                                         Creature user) {
        // Pick first alive hostile creature using Team
        return allCombatants.stream()
            .filter(c -> c.isAlive() && c.isHostileTo(user))
            .limit(1)
            .collect(Collectors.toList());
    }

    @Override
    public boolean shouldUseReaction(OngoingEffect trigger, EffectContext context) {
        return true; // Always use reactions for now
    }
}

// Future: AgenticBehaviorStrategy that calls LangChain/LangGraph or other agent framework
```

---

## File Structure

The project is organized with one class per file for maintainability:

```
dnd-combat-simulator/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── dndcombat/
│                   ├── Main.java
│                   │
│                   ├── core/
│                   │   ├── creatures/
│                   │   │   ├── Creature.java
│                   │   │   ├── PlayerCharacter.java
│                   │   │   ├── Monster.java
│                   │   │   ├── AbilityScores.java
│                   │   │   ├── SavingThrows.java
│                   │   │   └── Skills.java
│                   │   │
│                   │   ├── combat/
│                   │   │   ├── CombatManager.java
│                   │   │   ├── ActionResult.java
│                   │   │   └── CombatLog.java
│                   │   │
│                   │   ├── context/
│                   │   │   ├── EffectContext.java
│                   │   │   ├── AttackContext.java
│                   │   │   ├── SaveContext.java
│                   │   │   ├── DamageContext.java
│                   │   │   └── DamageComponent.java
│                   │   │
│                   │   └── dice/
│                   │       └── DiceRoller.java
│                   │
│                   ├── actions/
│                   │   ├── Action.java
│                   │   ├── ActionType.java
│                   │   ├── TargetingRequirements.java
│                   │   │
│                   │   ├── attacks/
│                   │   │   ├── WeaponAttack.java
│                   │   │   └── UnarmedStrike.java
│                   │   │
│                   │   ├── spells/
│                   │   │   ├── Spell.java
│                   │   │   ├── Fireball.java
│                   │   │   ├── Bane.java
│                   │   │   └── School.java
│                   │   │
│                   │   └── features/
│                   │       ├── Rage.java
│                   │       ├── ActionSurge.java
│                   │       └── SecondWind.java
│                   │
│                   ├── effects/
│                   │   ├── OngoingEffect.java
│                   │   ├── EffectHook.java
│                   │   │
│                   │   ├── buffs/
│                   │   │   ├── RageBuff.java
│                   │   │   ├── ShieldBuff.java
│                   │   │   ├── AbsorbElementsBuff.java
│                   │   │   └── BlessBuff.java
│                   │   │
│                   │   ├── debuffs/
│                   │   │   ├── BaneDebuff.java
│                   │   │   ├── StunnedCondition.java
│                   │   │   └── PoisonedCondition.java
│                   │   │
│                   │   └── triggers/
│                   │       ├── ShieldReactionTrigger.java
│                   │       ├── AbsorbElementsReactionTrigger.java
│                   │       └── OpportunityAttackTrigger.java
│                   │
│                   ├── equipment/
│                   │   ├── Weapon.java
│                   │   ├── WeaponProperty.java
│                   │   ├── WeaponMastery.java
│                   │   ├── Armor.java
│                   │   ├── Equipment.java
│                   │   │
│                   │   ├── weapons/
│                   │   │   ├── Weapons.java (contains constants like LONGSWORD, GREATSWORD)
│                   │   │   └── FlameTongue.java
│                   │   │
│                   │   └── masteries/
│                   │       ├── LongswordMastery.java
│                   │       ├── GreataxeMastery.java
│                   │       └── other mastery implementations...
│                   │
│                   ├── enums/
│                   │   ├── AbilityType.java
│                   │   ├── AttackType.java
│                   │   ├── DamageType.java
│                   │   ├── CreatureType.java
│                   │   ├── CharacterClass.java
│                   │   ├── ResourceType.java
│                   │   ├── CastingTime.java
│                   │   └── Team.java
│                   │
│                   ├── ai/
│                   │   ├── BehaviorStrategy.java
│                   │   ├── SimpleBehaviorStrategy.java
│                   │   └── AgenticBehaviorStrategy.java  (for future)
│                   │
│                   ├── ui/
│                   │   ├── InputHandler.java
│                   │   └── CLIInputHandler.java
│                   │
│                   └── resources/
│                       ├── SpellSlots.java
│                       └── Feature.java
│
└── test/
    └── java/
        └── com/
            └── dndcombat/
                ├── core/
                │   ├── dice/
                │   │   └── DiceRollerTest.java
                │   └── context/
                │       ├── AttackContextTest.java
                │       ├── SaveContextTest.java
                │       └── DamageContextTest.java
                ├── actions/
                │   └── attacks/
                │       └── WeaponAttackTest.java
                └── effects/
                    ├── BaneDebuffTest.java
                    └── triggers/
                        └── ShieldReactionTriggerTest.java
```

**Organizational Principles:**
- **`core/`** - Foundational classes (creatures, combat management, dice)
- **`core/context/`** - Context hierarchy (`EffectContext`, `AttackContext`, `SaveContext`, `DamageContext`)
- **`actions/`** - All action types organized by category
- **`effects/`** - Ongoing effects split into buffs/debuffs
- **`effects/triggers/`** - Reaction triggers (persistent effects that prompt for reactions)
- **`equipment/`** - Weapons (including magic weapon subclasses like `FlameTongue`), armor, and weapon mastery
- **`enums/`** - All enums in one place (including `Team`)
- **`ai/`** - Behavior strategies for monster AI
- **`ui/`** - Input handlers and interface code
- **`resources/`** - Supporting classes for resource management

---

## Key Design Decisions

### 1. **Effect Hook System with Priority**
- Each `OngoingEffect` registers for specific hooks (START_OF_TURN, BEFORE_ATTACK_ROLL, etc.)
- When events happen, the combat manager or action execution triggers relevant hooks
- Effects fire in **priority order** within each hook (lower number = fires first)
- Priority constants: `PRIORITY_EARLY` (25) → `PRIORITY_NORMAL` (100) → `PRIORITY_LATE` (175) → `PRIORITY_REACTION` (200)
- This ensures modifying effects (Bane subtracting 1d4, Bless adding 1d4) resolve before reaction triggers (Shield deciding whether to fire), giving reactions full information about the current state
- Effects with equal priority fire in the order they were added to `activeEffects`
- Efficient: only effects that registered for a specific hook get called

### 2. **Typed Context Hierarchy**
- The base `EffectContext` represents THE ACTION/EVENT being resolved, not the creature performing it
- Actions create typed sub-contexts for each resolution phase:
  - **`AttackContext`** — attack type (melee/ranged, weapon/spell), attack roll, advantage/disadvantage, critical, roll modifiers, attack negation. Deliberately weapon-agnostic — the Action knows what's generating the attack
  - **`SaveContext`** — save ability, DC, roll, success/fail, force reroll
  - **`DamageContext`** — list of `DamageComponent`s (each with type + amount), per-type resistance/immunity/vulnerability. One context = one damage instance = one concentration save
- Multiple creatures modify the **same** typed context as it flows through hooks
- Each hook has a documented context type (see Hook-to-Context Type Mapping) — effects can safely cast
- The `contextData` map remains as an escape hatch for truly unusual cases (e.g., Rage's spell cancellation), but 90% of uses are now typed fields
- **One damage calculation path**: `DamageContext.calculateFinalDamage()` processes all components with per-type resistance and returns the total — no competing mechanisms
- **Multi-type damage**: A Flame Tongue adds fire as a component to the same `DamageContext` as the slashing. Rage halves the slashing but not the fire. The total determines concentration DC
- For multi-target spells, create a **new** `SaveContext` and `DamageContext` per target for complete isolation

### 3. **Team-Based Faction System**
- Each creature has a `Team` enum (`PARTY`, `ENEMIES`, `NEUTRAL`) instead of relying on class hierarchies
- `Team.isHostileTo()` determines hostility: PARTY ↔ ENEMIES are hostile, NEUTRAL is hostile to nobody
- `Creature.isHostileTo(other)` delegates to the Team check
- Replaces all `instanceof PlayerCharacter` / `instanceof Enemy` checks in CombatManager
- Supports future scenarios: charmed creatures switching teams, PvP, neutral NPCs, temporary alliances
- Combat end condition checks `Team.PARTY` vs `Team.ENEMIES` alive counts

### 4. **Weapon System: Data + onHit**
- Standard weapons are data objects (instances of `Weapon`)
- Magic weapons with special on-hit effects subclass `Weapon` and override `onHit(DamageContext)` — the behavior stays WITH the weapon, not in a separate `WeaponAttack` subclass
- One `WeaponAttack` class works with ALL weapons (mundane and magic) — it calls `weapon.onHit(damageCtx)` during the damage phase
- `onHit()` fires after base damage is rolled but before `BEFORE_DAMAGE` hooks, so effects like Rage resistance see all damage components
- Magic bonuses (+1/+2/+3) are properties on the base `Weapon` class
- Weapon mastery uses Strategy pattern for extensibility (receives `AttackContext`)
- Benefits:
  - Easy to create new standard weapons (just new data)
  - Simple +X magic weapons are just instances with `setMagicBonus()`
  - Special magic weapons (FlameTongue, Vorpal, etc.) subclass `Weapon` — all on-hit logic is in one place
  - Could load standard weapons from JSON later
  - No proliferation of `WeaponAttack` subclasses — one attack action works for everything

### 5. **Calculate-on-Fly with Context**
- Methods like `getAttackBonus()` calculate dynamically, considering:
  - Base stats (ability modifiers, proficiency)
  - Equipment bonuses (weapon +1, +2, etc.)
  - Active effects (buffs/debuffs via typed contexts)
- No need to track state changes manually
- Can add caching layer if performance becomes an issue

### 6. **Action Execution Flow (with Typed Contexts)**
```
1. Player/AI chooses action and targets
2. CombatManager.executeAction() creates base EffectContext
3. Pass base context to Action.execute(context)
4. Inside action execution:
   a. Create AttackContext from base context
   b. Trigger BEFORE_ATTACK_ROLL on attacker (modifies AttackContext)
   c. Trigger WHEN_BEING_ATTACKED on target (modifies same AttackContext)
   d. Make roll using AttackContext (handles advantage/disadvantage)
   e. Trigger WHEN_HIT_BY_ATTACK on target (reaction triggers like Shield fire here)
   f. If attack negated by reaction, return miss
   g. Create DamageContext from base context
   g2. Call weapon.onHit(damageCtx) — magic weapon effects add components here
   h. Trigger BEFORE_DAMAGE_DEALT on attacker (modifies DamageContext)
   i. Trigger BEFORE_DAMAGE_TAKEN on target (modifies same DamageContext)
   j. Calculate final damage via DamageContext.calculateFinalDamage()
   k. Deal damage to target
5. Check concentration breaks
6. Mark action as used

Key: Multiple creatures modify the SAME typed context throughout each phase
```

### 7. **Reaction Trigger Pattern**
- Reactions are **two objects**: a persistent **trigger** (`OngoingEffect` with `PRIORITY_REACTION`) and a temporary **buff/debuff** it creates when accepted
- Triggers live in the creature's `activeEffects` with `remainingDuration = -1` (permanent) and fire through the normal hook dispatch — no special reaction system needed
- When a trigger's hook fires, it: (1) checks prerequisites (reaction available, resources), (2) prompts the creature's controller via `CombatManager.promptReaction()`, (3) if accepted, consumes resources and creates the buff
- Because triggers use `PRIORITY_REACTION`, they fire **after** all modifying effects, giving the player/AI full information before deciding
- The buff is a standard `OngoingEffect` with its own hooks and priority (e.g., `ShieldBuff` at `PRIORITY_EARLY` for AC)
- This pattern works for all reactions: Shield (WHEN_HIT_BY_ATTACK), Absorb Elements (BEFORE_DAMAGE_TAKEN), Counterspell (WHEN_CASTING_SPELL), Silvery Barbs (AFTER_ENEMY_SAVING_THROW), Opportunity Attacks (custom movement hook)
- No separate `availableReactions` list on Creature — everything flows through `activeEffects`

### 8. **Extensibility Points**

**For Future UI:**
- Replace `InputHandler` implementation
- Same `CombatManager` core logic
- Add event listeners for UI updates

**For Future AI Agent:**
- Implement `BehaviorStrategy` interface
- Hook into LangChain/LangGraph
- Agent can query game state, available actions, and make decisions
- No changes needed to core combat logic

**For Future Map System:**
- Add `Position` field to `Creature`
- Add distance checking to `isValidTarget()`
- Extend `TargetingRequirements` with range/area properties
- No changes to action execution flow

---

## Example: Complete Combat Flow with Typed Contexts

### Barbarian with Rage and Reckless Attack vs Goblin

```java
public static void main(String[] args) {
    // Create party
    PlayerCharacter barbarian = new Barbarian("Grok", 5);
    barbarian.setTeam(Team.PARTY);
    barbarian.addEffect(new RageBuff(10));               // Rage active
    barbarian.addEffect(new RecklessAttackBuff(1));       // Used reckless attack this turn
    barbarian.addAction(new WeaponAttack(Weapons.GREATSWORD));

    // Create monster
    Monster goblin = new Goblin("Sneaky Pete");
    goblin.setTeam(Team.ENEMIES);

    // Set up combat
    CombatManager combat = new CombatManager();
    combat.addCombatant(barbarian);
    combat.addCombatant(goblin);

    // Start combat - this is what happens:
    combat.startCombat();
}
```

### What Actually Happens (Barbarian's Turn):

```java
// 1. CombatManager creates BASE context for the attack
EffectContext context = new EffectContext(
    combatManager,
    greatswordAttack,  // The action
    barbarian,         // The attacker
    List.of(goblin)    // The target
);

// 2. Execute the attack
greatswordAttack.execute(context);

// Inside WeaponAttack.execute():

// 3. Create TYPED AttackContext for the attack phase
AttackContext attackCtx = new AttackContext(context, AttackType.MELEE_WEAPON);

// 4. Trigger BEFORE_ATTACK_ROLL on barbarian
barbarian.triggerEffects(EffectHook.BEFORE_ATTACK_ROLL, attackCtx);
// Effects fire in priority order:
// - RageBuff (priority 100): Not registered for this hook, skipped
// - RecklessAttackBuff (priority 100): Registered! Calls attackCtx.grantAdvantage()
// - attackCtx.hasAdvantage is now TRUE

// 5. Trigger WHEN_BEING_ATTACKED on goblin
goblin.triggerEffects(EffectHook.WHEN_BEING_ATTACKED, attackCtx);
// - Goblin has no effects registered for this hook
// - attackCtx.hasAdvantage still TRUE

// 6. Make attack roll with advantage (handled by AttackContext)
int attackRoll = attackCtx.rollAttack();
// Internally: Math.max(DiceRoller.d20(), DiceRoller.d20()) → 17

int totalAttack = attackCtx.applyRollModifiers(17 + 7);  // 24 (no roll modifiers)
attackCtx.setAttackRoll(17);
attackCtx.setAttackTotal(24);
attackCtx.setCritical(false);  // not a nat 20

// 7. Check if hit (goblin AC is 15, so yes!)
// 8. Trigger WHEN_HIT_BY_ATTACK on goblin
goblin.triggerEffects(EffectHook.WHEN_HIT_BY_ATTACK, attackCtx);
// - No reaction triggers on goblin, nothing happens
// - attackCtx.isAttackNegated() is false

// 9. Create TYPED DamageContext for the damage phase
//    Single component: [slashing: 14]
int damageRoll = 10;  // Rolled 2d6 for greatsword
int damageBonus = 4;  // STR modifier
DamageContext damageCtx = new DamageContext(context, 14, DamageType.SLASHING);

// 10. Trigger BEFORE_DAMAGE_DEALT on barbarian
barbarian.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);
// - RageBuff (priority 100): Registered! Checks melee → calls damageCtx.addModifier(+2)
// - Primary component (slashing) is now 16

// 11. Trigger BEFORE_DAMAGE_TAKEN on goblin
goblin.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);
// - No effects, nothing happens

// 12. Calculate final damage — processes all components with per-type resistance
int totalDamage = damageCtx.calculateFinalDamage();
// Components: [slashing: 16] → no resistances → total = 16

// 13. Apply damage (one instance, one concentration save if applicable)
goblin.takeDamage(16);
```

### What Happens When Goblin Attacks Back (with Rage Resistance):

```java
// Goblin's turn — CombatManager creates base context
EffectContext context = new EffectContext(
    combatManager,
    goblinAttack,
    goblin,            // Goblin is attacker
    List.of(barbarian) // Barbarian is target
);

goblinAttack.execute(context);

// Inside WeaponAttack.execute():

// AttackContext created — weapon-agnostic, just knows it's a melee weapon attack
AttackContext attackCtx = new AttackContext(context, AttackType.MELEE_WEAPON);

// Trigger BEFORE_ATTACK_ROLL on goblin
goblin.triggerEffects(EffectHook.BEFORE_ATTACK_ROLL, attackCtx);
// - No effects

// Trigger WHEN_BEING_ATTACKED on barbarian
barbarian.triggerEffects(EffectHook.WHEN_BEING_ATTACKED, attackCtx);
// - RecklessAttackBuff (priority 100): Registered! Calls attackCtx.grantAdvantage()
// - Goblin gets advantage against reckless barbarian!

int attackRoll = attackCtx.rollAttack();  // Advantage!
// Math.max(d20(), d20()) → 18

// ... attack hits, create DamageContext ...

// Single component: [piercing: 5]
DamageContext damageCtx = new DamageContext(context, 5, DamageType.PIERCING);

// Trigger BEFORE_DAMAGE_DEALT on goblin
goblin.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);
// - No effects

// Trigger BEFORE_DAMAGE_TAKEN on barbarian
barbarian.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);
// - RageBuff (priority 100): Registered! Calls damageCtx.addPhysicalResistance()
//   Adds BLUDGEONING, PIERCING, SLASHING to resistedTypes

int totalDamage = damageCtx.calculateFinalDamage();
// Components: [piercing: 5] → piercing is resisted → 5/2 = 2
// total = 2 (halved by Rage!)

barbarian.takeDamage(2);
```

### Shield Reaction in Action (Wizard vs Goblin Attack):

```java
// Setup: Wizard has Shield prepared → ShieldReactionTrigger in activeEffects
wizard.addEffect(new ShieldReactionTrigger(wizard));  // priority 200, permanent

// Goblin attacks wizard
EffectContext context = new EffectContext(combatManager, goblinAttack, goblin, List.of(wizard));
goblinAttack.execute(context);

// Inside WeaponAttack.execute():
AttackContext attackCtx = new AttackContext(context, AttackType.MELEE_WEAPON);

// ... attack roll happens, total = 16, wizard AC = 13, it's a hit ...

// Trigger WHEN_HIT_BY_ATTACK on wizard (effects fire in priority order)
wizard.triggerEffects(EffectHook.WHEN_HIT_BY_ATTACK, attackCtx);
// - ShieldReactionTrigger (priority 200): Registered!
//   1. Checks: wizard.isReactionUsed()? No. wizard.hasSpellSlot(1)? Yes.
//   2. Calls combatManager.promptReaction(wizard, this, attackCtx)
//      → CLI shows: "Use Shield (Reaction)? (Reaction)" → Player says yes
//   3. wizard.consumeSpellSlot(1)
//   4. wizard.setReactionUsed(true)
//   5. wizard.addEffect(new ShieldBuff(wizard))  ← +5 AC
//   6. Checks: attackCtx.getAttackTotal() (16) < wizard.getArmorClass() (now 18)?
//      → YES! Calls attackCtx.setAttackNegated(true)

// Back in WeaponAttack.execute():
if (attackCtx.isAttackNegated()) {
    return ActionResult.miss();  // Shield blocked it!
}
```

### Wizard Casts Fireball (Multiple Targets, Typed Contexts):

```java
EffectContext context = new EffectContext(
    combatManager,
    fireballSpell,
    wizard,
    List.of(goblin1, goblin2, goblin3)  // 3 targets
);

fireballSpell.execute(context);

// Inside Fireball.execute():
int baseDamage = DiceRoller.roll(8, 6);  // 28 damage
int saveDC = getSpellSaveDC(wizard);     // 15

for (Creature target : List.of(goblin1, goblin2, goblin3)) {
    // Fresh SaveContext per target — complete isolation
    SaveContext saveCtx = new SaveContext(context, AbilityType.DEXTERITY, saveDC);

    // Target makes save
    target.triggerEffects(EffectHook.BEFORE_SAVING_THROW, saveCtx);
    // If target has Bane: saveCtx.addModifier(-d4) via typed field

    int saveRoll = DiceRoller.d20();
    int totalSave = saveCtx.applyModifiers(saveRoll + target.getDexBonus());
    saveCtx.setSaveRoll(saveRoll);
    saveCtx.setSaveTotal(totalSave);
    saveCtx.setSucceeded(totalSave >= saveDC);

    // Check for Silvery Barbs — uses saveCtx.isForceReroll() (typed field)
    for (Creature other : combatManager.getAllCombatants()) {
        if (other == target || !other.hasReactionAvailable()) continue;
        if (!other.isHostileTo(target)) continue;  // Team-based check

        other.triggerEffects(EffectHook.AFTER_ENEMY_SAVING_THROW, saveCtx);

        if (saveCtx.isForceReroll()) {
            // ... reroll logic using typed fields ...
            break;
        }
    }

    // Fresh DamageContext per target — single fire component
    int damage = saveCtx.isSucceeded() ? baseDamage / 2 : baseDamage;
    DamageContext damageCtx = new DamageContext(context, damage, DamageType.FIRE);

    // AbsorbElementsReactionTrigger fires here if target has it
    target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);
    // Calls damageCtx.addResistance(FIRE) — halves the fire component

    int totalDamage = damageCtx.calculateFinalDamage();
    target.takeDamage(totalDamage);
}
```

---

## Next Steps

8. **Phase 8**: Create creature/ability definitions (fighter, wizard, common spells)
9. **Phase 9**: Testing and balancing
10. **Phase 10** (Future): AI agent integration
11. **Phase 11** (Future): GUI/web interface

TODOs:
- fix the action system to allow bonus actions before or after the main action
- fix how concentration works to match D&D (right now it just removes the buffs from self not enemeies)