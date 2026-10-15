package myau.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import myau.command.CommandRegistry;
import myau.command.impl.BindCmd;
import myau.command.impl.ConfigCmd;
import myau.command.impl.DenickCmd;
import myau.command.impl.EnemyCmd;
import myau.command.impl.FriendCmd;
import myau.command.impl.HelpCmd;
import myau.command.impl.HideCmd;
import myau.command.impl.ItemNameCmd;
import myau.command.impl.ModuleListCmd;
import myau.command.impl.ModuleSettingsCmd;
import myau.command.impl.PlayerListCmd;
import myau.command.impl.RevertChatColorCmd;
import myau.command.impl.ShowCmd;
import myau.command.impl.ToggleCmd;
import myau.command.impl.UsernameCmd;
import myau.command.impl.VClipCmd;
import myau.event.EventDispatcher;
import myau.module.ClientModule;
import myau.module.ModuleRegistry;
import myau.module.combat.AimAssist;
import myau.module.combat.AntiFireball;
import myau.module.combat.AutoClicker;
import myau.module.combat.HitSelect;
import myau.module.combat.KeepSprint;
import myau.module.combat.KillAura;
import myau.module.combat.NoHitDelay;
import myau.module.combat.Reach;
import myau.module.combat.Velocity;
import myau.module.combat.WTap;
import myau.module.misc.AntiObfuscate;
import myau.module.misc.Blink;
import myau.module.misc.GameTimer;
import myau.module.misc.LagRange;
import myau.module.misc.MiddleClickFriend;
import myau.module.misc.NickHider;
import myau.module.misc.NoRotate;
import myau.module.misc.Spammer;
import myau.module.movement.AntiVoid;
import myau.module.movement.Eagle;
import myau.module.movement.Fly;
import myau.module.movement.InvWalk;
import myau.module.movement.Jesus;
import myau.module.movement.LongJump;
import myau.module.movement.NoJumpDelay;
import myau.module.movement.NoSlow;
import myau.module.movement.SafeWalk;
import myau.module.movement.Speed;
import myau.module.movement.Sprint;
import myau.module.movement.TargetStrafe;
import myau.module.player.AntiDebuff;
import myau.module.player.AutoHeal;
import myau.module.player.AutoTool;
import myau.module.player.ChestStealer;
import myau.module.player.GhostHand;
import myau.module.player.InvManager;
import myau.module.player.NoFall;
import myau.module.render.BedESP;
import myau.module.render.BedTracker;
import myau.module.render.Chams;
import myau.module.render.ChestESP;
import myau.module.render.ESP;
import myau.module.render.Fullbright;
import myau.module.render.GuiModule;
import myau.module.render.HeadsUpDisplay;
import myau.module.render.Indicators;
import myau.module.render.ItemESP;
import myau.module.render.LightningTracker;
import myau.module.render.NameTags;
import myau.module.render.NoHurtCam;
import myau.module.render.TargetDisplay;
import myau.module.render.Tracers;
import myau.module.render.Trajectories;
import myau.module.render.ViewClip;
import myau.module.render.Xray;
import myau.module.world.AntiObbyTrap;
import myau.module.world.BedNuker;
import myau.module.world.FastPlace;
import myau.module.world.Scaffold;
import myau.module.world.SpeedMine;
import myau.network.InboundPacketQueue;
import myau.network.OutboundPacketQueue;
import myau.network.PacketActionLog;
import myau.network.PacketDelayer;
import myau.player.GroundSpoofController;
import myau.player.HurtTracker;
import myau.rotation.RotationController;
import myau.setting.Option;
import myau.setting.OptionStore;
import myau.social.EnemyNames;
import myau.social.FriendNames;

public final class ClientContext {
   public static final String CHAT_PREFIX = "&7[&bMyau&7] &r";
   public static EventDispatcher eventBus;
   public static RotationController rotationSmoother;
   public static GroundSpoofController groundSpoofManager;
   public static HurtTracker hurtTimeTracker;
   public static OutboundPacketQueue outgoingPacketBuffer;
   public static InboundPacketQueue incomingPacketBuffer;
   public static PacketDelayer packetDelayManager;
   public static PacketActionLog packetActionTracker;
   public static FriendNames friendList;
   public static EnemyNames enemyList;
   public static OptionStore settingManager;
   public static ModuleRegistry moduleManager;
   public static CommandRegistry commandManager;
   private static boolean initialized;

   private ClientContext() {
   }

   public static synchronized void initialize() {
      if (!initialized) {
         eventBus = new EventDispatcher();
         rotationSmoother = new RotationController();
         groundSpoofManager = new GroundSpoofController();
         hurtTimeTracker = new HurtTracker();
         outgoingPacketBuffer = new OutboundPacketQueue();
         incomingPacketBuffer = new InboundPacketQueue();
         packetDelayManager = new PacketDelayer();
         packetActionTracker = new PacketActionLog();
         friendList = new FriendNames();
         enemyList = new EnemyNames();
         settingManager = new OptionStore();
         moduleManager = new ModuleRegistry();
         commandManager = new CommandRegistry();
         registerInfrastructureListeners();
         registerModules();
         registerCommands();
         friendList.load();
         enemyList.load();
         initialized = true;
      }
   }

   private static void registerInfrastructureListeners() {
      eventBus.register(rotationSmoother);
      eventBus.register(groundSpoofManager);
      eventBus.register(hurtTimeTracker);
      eventBus.register(outgoingPacketBuffer);
      eventBus.register(incomingPacketBuffer);
      eventBus.register(packetDelayManager);
      eventBus.register(commandManager);
   }

   private static void registerModules() {
      registerModule(new AimAssist());
      registerModule(new AntiDebuff());
      registerModule(new AntiFireball());
      registerModule(new AntiObbyTrap());
      registerModule(new AntiObfuscate());
      registerModule(new AntiVoid());
      registerModule(new AutoClicker());
      registerModule(new AutoHeal());
      registerModule(new AutoTool());
      registerModule(new BedESP());
      registerModule(new BedNuker());
      registerModule(new BedTracker());
      registerModule(new Blink());
      registerModule(new Chams());
      registerModule(new ChestESP());
      registerModule(new ChestStealer());
      registerModule(new Eagle());
      registerModule(new ESP());
      registerModule(new FastPlace());
      registerModule(new Fly());
      registerModule(new Fullbright());
      registerModule(new GhostHand());
      registerModule(new GuiModule());
      registerModule(new HitSelect());
      registerModule(new HeadsUpDisplay());
      registerModule(new Indicators());
      registerModule(new InvManager());
      registerModule(new InvWalk());
      registerModule(new ItemESP());
      registerModule(new Jesus());
      registerModule(new KeepSprint());
      registerModule(new KillAura());
      registerModule(new LagRange());
      registerModule(new LightningTracker());
      registerModule(new LongJump());
      registerModule(new MiddleClickFriend());
      registerModule(new NameTags());
      registerModule(new NickHider());
      registerModule(new NoFall());
      registerModule(new NoHitDelay());
      registerModule(new NoHurtCam());
      registerModule(new NoJumpDelay());
      registerModule(new NoRotate());
      registerModule(new NoSlow());
      registerModule(new Reach());
      registerModule(new SafeWalk());
      registerModule(new Scaffold());
      registerModule(new Spammer());
      registerModule(new Speed());
      registerModule(new SpeedMine());
      registerModule(new Sprint());
      registerModule(new TargetDisplay());
      registerModule(new TargetStrafe());
      registerModule(new GameTimer());
      registerModule(new Tracers());
      registerModule(new Trajectories());
      registerModule(new Velocity());
      registerModule(new ViewClip());
      registerModule(new WTap());
      registerModule(new Xray());
   }

   private static void registerModule(ClientModule module) {
      moduleManager.modules.put(module.getClass(), module);
      eventBus.register(module);
      ArrayList<Option<?>> settings = new ArrayList<>();

      for (Field field : module.getClass().getFields()) {
         if (Option.class.isAssignableFrom(field.getType())) {
            try {
               Option<?> setting = (Option<?>)field.get(module);
               if (setting != null) {
                  setting.setModule(module);
                  settings.add(setting);
               }
            } catch (IllegalAccessException var7) {
               throw new IllegalStateException("Unable to register setting " + field.getName(), var7);
            }
         }
      }

      settingManager.settingsByModule.put(module.getClass(), settings);
   }

   private static void registerCommands() {
      commandManager.commands.add(new BindCmd());
      commandManager.commands.add(new ConfigCmd());
      commandManager.commands.add(new DenickCmd());
      commandManager.commands.add(new EnemyCmd());
      commandManager.commands.add(new FriendCmd());
      commandManager.commands.add(new HelpCmd());
      commandManager.commands.add(new HideCmd());
      commandManager.commands.add(new ItemNameCmd());
      commandManager.commands.add(new ModuleListCmd());
      commandManager.commands.add(new ModuleSettingsCmd());
      commandManager.commands.add(new PlayerListCmd());
      commandManager.commands.add(new RevertChatColorCmd());
      commandManager.commands.add(new ShowCmd());
      commandManager.commands.add(new ToggleCmd());
      commandManager.commands.add(new UsernameCmd());
      commandManager.commands.add(new VClipCmd());
   }
}
