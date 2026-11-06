# Minecraft API surface used by the Sit mod, written with Mojang official names.
# class <Key> <mojmap class names separated by |>
# method <Key>.<id> <argc> <declaring class keys separated by |> <mojmap method names separated by |>
# field  <Key>.<id> <declaring class keys separated by |> <mojmap field names separated by |>

class Entity net.minecraft.world.entity.Entity
class Mob net.minecraft.world.entity.Mob
class LivingEntity net.minecraft.world.entity.LivingEntity
class Player net.minecraft.world.entity.player.Player
class EntityType net.minecraft.world.entity.EntityType
class EntitySpawnReason net.minecraft.world.entity.EntitySpawnReason
class Level net.minecraft.world.level.Level
class BlockPos net.minecraft.core.BlockPos
class Vec3i net.minecraft.core.Vec3i
class Block net.minecraft.world.level.block.Block
class BlockState net.minecraft.world.level.block.state.BlockState
class StateHolder net.minecraft.world.level.block.state.StateHolder
class BlockBehaviourState net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase
class Property net.minecraft.world.level.block.state.properties.Property
class StringRepresentable net.minecraft.util.StringRepresentable
class ItemStack net.minecraft.world.item.ItemStack
class InteractionHand net.minecraft.world.InteractionHand
class InteractionResult net.minecraft.world.InteractionResult
class Component net.minecraft.network.chat.Component
class TextComponent net.minecraft.network.chat.TextComponent
class CommandSourceStack net.minecraft.commands.CommandSourceStack
class MinecraftServer net.minecraft.server.MinecraftServer
class ServerLevel net.minecraft.server.level.ServerLevel
class ServerPlayer net.minecraft.server.level.ServerPlayer
class Pig net.minecraft.world.entity.animal.Pig
class StairBlock net.minecraft.world.level.block.StairBlock
class SlabBlock net.minecraft.world.level.block.SlabBlock
class FenceBlock net.minecraft.world.level.block.FenceBlock
class WallBlock net.minecraft.world.level.block.WallBlock
class FenceGateBlock net.minecraft.world.level.block.FenceGateBlock
class DoorBlock net.minecraft.world.level.block.DoorBlock
class TrapDoorBlock net.minecraft.world.level.block.TrapDoorBlock
class BedBlock net.minecraft.world.level.block.BedBlock
class ChestBlock net.minecraft.world.level.block.ChestBlock
class LadderBlock net.minecraft.world.level.block.LadderBlock

# --- entity ---
method Entity.moveTo 5 Entity moveTo|snapTo
method Entity.setPos 3 Entity setPos
method Entity.setNoGravity 1 Entity setNoGravity
method Entity.setInvulnerable 1 Entity setInvulnerable|setPermanentlyInvulnerable
method Entity.setSilent 1 Entity setSilent
method Entity.setInvisible 1 Entity setInvisible
method Entity.startRiding1 1 Entity startRiding
method Entity.startRiding2 2 Entity startRiding
method Entity.startRiding3 3 Entity startRiding
method Entity.isShiftKeyDown 0 Entity isShiftKeyDown|isCrouching|isSneaking
method Entity.isPassenger 0 Entity isPassenger
method Entity.isVehicle 0 Entity isVehicle
method Entity.getPassengers 0 Entity getPassengers
method Entity.discard 0 Entity discard
method Entity.remove0 0 Entity remove
method Entity.isRemoved 0 Entity|EntityAccess isRemoved
method Entity.addTag 1 Entity addTag
method Entity.getYRot 0 Entity getYRot
method Entity.setYRot 1 Entity setYRot
method Entity.createCommandSourceStack 0 Entity|ServerPlayer createCommandSourceStack
method Entity.getVehicle 0 Entity getVehicle
field Entity.yRot Entity yRot
field Entity.removed Entity removed
method Mob.setNoAi 1 Mob setNoAi
method LivingEntity.getMainHandItem 0 LivingEntity getMainHandItem
method ItemStack.isEmpty 0 ItemStack isEmpty
method Pig.setSaddle 1 Pig setSaddle
method Pig.equipSaddle 1 Pig|Saddleable equipSaddle

# --- entity creation ---
method EntityType.byString 1 EntityType byString
method EntityType.create1 1 EntityType create
method EntityType.create2 2 EntityType create
field EntitySpawnReason.COMMAND EntitySpawnReason COMMAND
field EntitySpawnReason.EVENT EntitySpawnReason EVENT

# --- world / blocks ---
field Level.isClientSide Level isClientSide
method Level.isClientSide 0 Level|LevelReader isClientSide
method Level.getBlockState 1 Level|BlockGetter getBlockState
method Level.addFreshEntity 1 Level|LevelWriter addFreshEntity
method Vec3i.getX 0 Vec3i getX
method Vec3i.getY 0 Vec3i getY
method Vec3i.getZ 0 Vec3i getZ
method Block.getDescriptionId 0 Block getDescriptionId
method BlockState.getBlock 0 BlockState|BlockBehaviourState getBlock
method StateHolder.getValues 0 StateHolder|BlockState|BlockBehaviourState getValues
method Property.getName 0 Property getName
method StringRepresentable.getSerializedName 0 StringRepresentable getSerializedName

# --- commands / messages ---
method CommandSourceStack.hasPermission 1 CommandSourceStack|SharedSuggestionProvider hasPermission
method CommandSourceStack.sendSuccess2 2 CommandSourceStack sendSuccess
method CommandSourceStack.sendSystemMessage 1 CommandSourceStack sendSystemMessage
method CommandSourceStack.sendMessage3 3 CommandSourceStack sendMessage
method Component.literal 1 Component literal
method MinecraftServer.isSingleplayer 0 MinecraftServer isSingleplayer

# --- permissions (1.21.11+) ---
class LevelWriter net.minecraft.world.level.LevelWriter
class Permissions net.minecraft.server.permissions.Permissions
class PermissionSet net.minecraft.server.permissions.PermissionSet
method CommandSourceStack.permissions 0 CommandSourceStack|SharedSuggestionProvider permissions
method PermissionSet.hasPermission 1 PermissionSet hasPermission
field Permissions.COMMANDS_GAMEMASTER Permissions COMMANDS_GAMEMASTER
class BlockGetter net.minecraft.world.level.BlockGetter
class LevelReader net.minecraft.world.level.LevelReader
class SharedSuggestionProvider net.minecraft.commands.SharedSuggestionProvider
class EntityAccess net.minecraft.world.level.entity.EntityAccess
class Saddleable net.minecraft.world.entity.Saddleable

# --- interaction result / hit result / registries ---
class BlockHitResult net.minecraft.world.phys.BlockHitResult
class ResourceLocation net.minecraft.resources.ResourceLocation|net.minecraft.resources.Identifier
class Registry net.minecraft.core.Registry
class BuiltInRegistries net.minecraft.core.registries.BuiltInRegistries
class DefaultedRegistry net.minecraft.core.DefaultedRegistry
class MappedRegistry net.minecraft.core.MappedRegistry
class PlayerList net.minecraft.server.players.PlayerList
method BlockHitResult.getBlockPos 0 BlockHitResult getBlockPos
field InteractionResult.SUCCESS InteractionResult SUCCESS
field InteractionResult.PASS InteractionResult PASS
field BuiltInRegistries.ENTITY_TYPE BuiltInRegistries ENTITY_TYPE
field Registry.ENTITY_TYPE Registry ENTITY_TYPE
method Registry.get1 1 Registry|DefaultedRegistry|MappedRegistry get|getOptional|getValue
method Registry.getOptional1 1 Registry|DefaultedRegistry|MappedRegistry getOptional
method Registry.getValue1 1 Registry|DefaultedRegistry|MappedRegistry getValue
method ResourceLocation.parse 1 ResourceLocation parse|tryParse
method MinecraftServer.getPlayerList 0 MinecraftServer getPlayerList

# --- seat persistence ---
method Entity.getTags 0 Entity getTags|entityTags
