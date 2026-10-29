package com.raidhub.client;

import com.raidhub.client.mixin.BossBarHudAccessor;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_465;
import net.minecraft.class_3675.class_307;

public final class RaidHelperClient implements ClientModInitializer {
   private static final String TICKET_NAME = "ve toi thuong";
   private static final String RAID_TITLE = "dot kich";
   private static final int MAX_WAIT_TICKS = 600;
   private static final int GUI_SETTLE_TICKS = 6;
   private static final int EMPTY_BOSS_MENU_GRACE_TICKS = 60;
   private static final int MIN_REPEAT_DELAY_TICKS = 80;
   private static final int MAX_REPEAT_DELAY_TICKS = 100;
   private static final int MAX_IMMEDIATE_CHAT_RETRIES = 3;
   private static final int BOSS_BAR_START_NOTICE_TICKS = 1200;
   private static final String SKIP_CONFIG_FILE = "raidhelper-skip-bosses.txt";
   private static final String STATS_FILE = "raidhelper-stats.properties";
   private static final String HISTORY_FILE = "raidhelper-history.csv";
   private static final String SETTINGS_FILE = "raidhelper-settings.properties";
   private static final int DEFAULT_COMPLETION_DELAY_MIN_SECONDS = 4;
   private static final int DEFAULT_COMPLETION_DELAY_MAX_SECONDS = 5;
   private static final int MAX_CONFIGURABLE_DELAY_SECONDS = 300;
   private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
   private static class_304 startKey;
   private static class_304 stopKey;
   private static class_304 configKey;
   private static class_304 hudKey;
   private static boolean hudVisible = true;
   private static boolean loopEnabled = false;
   private static int nextAttemptTicks = -1;
   private static boolean ticketWaitMessageShown = false;
   private static boolean barrierMenuRecoveryNoticeShown = false;
   private static int ticketHotbarSlot = -1;
   private static int ticketSwapPendingSlot = -1;
   private static int ticketSwapPendingTicks = 0;
   private static final int INVENTORY_SWAP_CONFIRM_TICKS = 12;
   private static boolean autoSkipFoughtBosses = true;
   private static boolean chatRetryPending = false;
   private static boolean chatRetryUseBackoff = false;
   private static String lastChatError = "";
   private static int consecutiveChatFailureRetries = 0;
   private static Path skipBossesPath;
   private static Set skippedBossTerms = Collections.emptySet();
   private static Path statsPath;
   private static Path historyPath;
   private static Path settingsPath;
   private static int completionDelayMinSeconds = 4;
   private static int completionDelayMaxSeconds = 5;
   private static BossSelectionMode bossSelectionMode;
   private static int lastOrderedBossSlotX;
   private static int lastOrderedBossSlotY;
   private static int lastOrderedBossSlotId;
   private static String statusText;
   private static String selectedBossName;
   private static long attemptsStarted;
   private static long bossSelections;
   private static long completedFights;
   private static long failedAttempts;
   private static long totalChatRetries;
   private static long totalFightMillis;
   private static long fightStartedAtMillis;
   private static final Map completedBossCounts;
   private static Phase phase;
   private static int ticksWaiting;
   private static class_465 entryScreen;
   private static String entryScreenTitle;
   private static String entryScreenFingerprint;
   private static boolean bossMenuReady;
   private static int ticksSinceBossMenuReady;
   private static Set bossBarsBeforeSelection;
   private static Set trackedRaidBossBars;
   private static boolean raidBossBarSeen;
   private static int ticksWaitingForBossBar;
   private static boolean bossBarStartNoticeShown;

   public void onInitializeClient() {
      ensureSkipListFile();
      reloadSkippedBosses();
      loadDelaySettings();
      loadStatistics();
      startKey = KeyBindingHelper.registerKeyBinding(new class_304("key.raidhelper.start", class_307.field_1668, 297, "category.raidhelper"));
      stopKey = KeyBindingHelper.registerKeyBinding(new class_304("key.raidhelper.stop", class_307.field_1668, 298, "category.raidhelper"));
      configKey = KeyBindingHelper.registerKeyBinding(new class_304("key.raidhelper.config", class_307.field_1668, 296, "category.raidhelper"));
      hudKey = KeyBindingHelper.registerKeyBinding(new class_304("key.raidhelper.hud", class_307.field_1668, 295, "category.raidhelper"));
      ClientTickEvents.END_CLIENT_TICK.register(RaidHelperClient::onClientTick);
      HudRenderCallback.EVENT.register(RaidHelperClient::renderHud);
   }

   private static void onClientTick(class_310 client) {
      while(hudKey.method_1436()) {
         hudVisible = !hudVisible;
         if (hudVisible) {
            message(client, "HUD hiện. Nhấn F6 để ẩn.");
         } else {
            message(client, "HUD đã ẩn. Nhấn F6 để hiện lại.");
         }
      }

      while(true) {
         while(configKey.method_1436()) {
            if (client.field_1724 != null && client.field_1755 == null && phase == RaidHelperClient.Phase.IDLE && !loopEnabled) {
               client.method_1507(new RaidHelperSkipScreen((class_437)null));
            } else if (client.field_1724 != null && !(client.field_1755 instanceof RaidHelperSkipScreen)) {
               message(client, "F7 chỉ dùng khi đã đóng menu và RaidHelper đang dừng.");
            }
         }

         while(startKey.method_1436()) {
            if (!(client.field_1755 instanceof RaidHelperSkipScreen)) {
               startLoop(client);
            }
         }

         while(stopKey.method_1436()) {
            stopLoop(client);
         }

         if (client.field_1724 != null && client.field_1687 != null) {
            if (!loopEnabled) {
               return;
            }

            if (chatRetryPending) {
               boolean useBackoff = chatRetryUseBackoff;
               chatRetryPending = false;
               chatRetryUseBackoff = false;
               handleChatFailureRetry(client, useBackoff);
               return;
            }

            if (!isPauseMenuOrSettingsScreen(client.field_1755) || phase != RaidHelperClient.Phase.WAIT_FOR_ENTRY_MENU && phase != RaidHelperClient.Phase.WAIT_FOR_BOSS_MENU) {
               if (phase == RaidHelperClient.Phase.IDLE) {
                  if (nextAttemptTicks < 0) {
                     return;
                  }

                  if (nextAttemptTicks > 0) {
                     --nextAttemptTicks;
                     return;
                  }

                  nextAttemptTicks = -1;
                  if (isPauseMenuOrSettingsScreen(client.field_1755)) {
                     client.method_1507((class_437)null);
                     nextAttemptTicks = 0;
                     statusText = "Đóng menu Esc để tiếp tục Đột Kích";
                     return;
                  }

                  if (client.field_1755 != null) {
                     statusText = "Chờ đóng giao diện Minecraft đang mở";
                     scheduleNextAttempt(client, "Menu đang mở; sẽ thử lại.");
                     return;
                  }

                  if (!ensureTicketInMainHand(client)) {
                     if (ticketSwapPendingSlot >= 0) {
                        scheduleTicketSwapPoll();
                        statusText = "Đang xác nhận chuyển Vé Tối Thượng vào hotbar";
                        return;
                     }

                     if (!ticketWaitMessageShown) {
                        message(client, "Không tìm thấy Vé Tối Thượng trong hotbar/kho đồ; sẽ tự kiểm tra lại.");
                        ticketWaitMessageShown = true;
                     }

                     scheduleNextAttempt(client, (String)null);
                     statusText = "Đang tìm Vé Tối Thượng trong kho đồ";
                     return;
                  }

                  ticketWaitMessageShown = false;
                  beginAttempt(client);
                  return;
               }

               if (phase == RaidHelperClient.Phase.WAIT_FOR_BOSS_DEATH) {
                  tickBossBarWait(client);
                  return;
               }

               ++ticksWaiting;
               if (ticksWaiting > 600) {
                  if (phase == RaidHelperClient.Phase.WAIT_FOR_ENTRY_MENU) {
                     finishAttempt(client, "Không thấy mục tham chiến; sẽ thử lại.");
                  } else if (!bossMenuReady) {
                     finishAttempt(client, "Danh sách boss chưa mở; sẽ thử lại.");
                  } else {
                     finishAttempt(client, "Không có boss phù hợp; sẽ thử lại.");
                  }

                  return;
               }

               if (ticksWaiting >= 6 && client.field_1755 instanceof class_465) {
                  class_465 screen = (class_465)client.field_1755;
                  String title;
                  int bossSlot;
                  if (phase == RaidHelperClient.Phase.WAIT_FOR_ENTRY_MENU) {
                     title = normalize(screen.method_25440().getString());
                     bossSlot = findWitherSkullSlot(screen);
                     if (bossSlot < 0) {
                        bossSlot = findJoinLabelSlot(screen);
                     }

                     if (!title.contains("dot kich") && bossSlot < 0) {
                        return;
                     }

                     if (bossSlot >= 0) {
                        entryScreen = screen;
                        entryScreenTitle = title;
                        entryScreenFingerprint = menuFingerprint(screen);
                        clickSlot(client, screen, bossSlot);
                        phase = RaidHelperClient.Phase.WAIT_FOR_BOSS_MENU;
                        ticksWaiting = 0;
                        bossMenuReady = false;
                        ticksSinceBossMenuReady = 0;
                        statusText = "Đã mở mục Đột Kích; chờ danh sách boss";
                     }

                     return;
                  }

                  if (phase == RaidHelperClient.Phase.WAIT_FOR_BOSS_MENU && !bossMenuReady) {
                     title = normalize(screen.method_25440().getString());
                     String nowFingerprint = menuFingerprint(screen);
                     boolean screenChanged = screen != entryScreen;
                     boolean titleChanged = !title.equals(entryScreenTitle);
                     boolean contentsChangedAndEntryGone = !nowFingerprint.equals(entryScreenFingerprint) && findWitherSkullSlot(screen) < 0 && findJoinLabelSlot(screen) < 0;
                     if (!screenChanged && !titleChanged && !contentsChangedAndEntryGone) {
                        return;
                     }

                     bossMenuReady = true;
                     ticksSinceBossMenuReady = 0;
                     statusText = "Đang tìm boss khả dụng";
                  }

                  if (phase == RaidHelperClient.Phase.WAIT_FOR_BOSS_MENU && bossMenuReady) {
                     List readySlots = findReadyBossSlots(screen);
                     if (!readySlots.isEmpty()) {
                        barrierMenuRecoveryNoticeShown = false;
                        bossSlot = chooseBossSlot(screen, readySlots);
                        Set existingBossBars = currentBossBarIds(client);
                        selectedBossName = getBossNameForSlot(screen, bossSlot);
                        ++bossSelections;
                        statusText = "Đã chọn boss (" + getBossSelectionModeLabel() + "); chờ thanh máu";
                        saveStatistics();
                        appendHistory("boss_selected", selectedBossName, "slot=" + bossSlot + ";mode=" + bossSelectionMode.name());
                        clickSlot(client, screen, bossSlot);
                        beginBossBarWait(client, existingBossBars);
                        return;
                     }

                     ++ticksSinceBossMenuReady;
                     if (ticksSinceBossMenuReady >= 60) {
                        if (isBarrierOnlyBossMenu(screen)) {
                           recoverFromBarrierOnlyMenu(client, screen);
                        } else {
                           finishAttempt(client, "Không có boss khả dụng; sẽ thử lại.");
                        }
                     }
                  }

                  return;
               }

               return;
            }

            client.method_1507((class_437)null);
            resetAttemptState();
            nextAttemptTicks = -1;
            statusText = "Đóng menu Esc để tiếp tục Đột Kích";
            beginAttempt(client);
            return;
         }

         if (loopEnabled) {
            loopEnabled = false;
            nextAttemptTicks = -1;
            resetAttemptState();
            ticketWaitMessageShown = false;
            chatRetryPending = false;
            chatRetryUseBackoff = false;
            consecutiveChatFailureRetries = 0;
            statusText = "Dừng do rời thế giới hoặc mất kết nối";
            fightStartedAtMillis = -1L;
            saveStatistics();
            appendHistory("loop_stopped", selectedBossName, "reason=world_unavailable");
         }

         return;
      }
   }

   private static boolean isPauseMenuOrSettingsScreen(class_437 screen) {
      if (screen == null) {
         return false;
      } else {
         String name = screen.getClass().getSimpleName();
         return "GameMenuScreen".equals(name) || "OptionsScreen".equals(name) || name.endsWith("OptionsScreen") || name.contains("ControlsOptions") || name.contains("KeyBinds") || name.contains("LanguageOptions") || name.contains("AccessibilityOptions");
      }
   }

   public static void onObservedChatMessage(String rawText) {
      if (loopEnabled && rawText != null && (phase != RaidHelperClient.Phase.WAIT_FOR_BOSS_DEATH || !raidBossBarSeen)) {
         String normalized = normalize(rawText);
         if (!normalized.contains("raidhelper") && isRaidNotFoundMessage(normalized)) {
            if (!chatRetryPending) {
               lastChatError = rawText;
               chatRetryUseBackoff = consecutiveChatFailureRetries >= 3;
               if (chatRetryUseBackoff) {
                  consecutiveChatFailureRetries = 0;
               } else {
                  ++consecutiveChatFailureRetries;
               }

               chatRetryPending = true;
            }
         }
      }
   }

   private static boolean isRaidNotFoundMessage(String text) {
      boolean mentionsRaid = text.contains("dot kich") || text.contains("dotkich") || text.contains("raid") || text.contains("dungeon") || text.contains("tham chien") || text.contains("tham gia") || text.contains("hoat dong");
      boolean notFound = text.contains("khong tim thay") || text.contains("khong tim duoc") || text.contains("khong the tim thay") || text.contains("not found") || text.contains("cannot find") || text.contains("could not find") || text.contains("couldn t find") || text.contains("no raid") || text.contains("no active") || text.contains("nothing found") || text.contains("raid not found");
      if (!notFound || !mentionsRaid && text.length() >= 25) {
         boolean unavailable = text.contains("khong co") || text.contains("khong ton tai") || text.contains("khong kha dung") || text.contains("khong the tham gia") || text.contains("cannot join") || text.contains("unable to join") || text.contains("unavailable") || text.contains("not available") || text.contains("there is no");
         return unavailable && mentionsRaid;
      } else {
         return true;
      }
   }

   private static void handleChatFailureRetry(class_310 client, boolean useBackoff) {
      if (loopEnabled && client.field_1724 != null && client.field_1687 != null && (phase != RaidHelperClient.Phase.WAIT_FOR_BOSS_DEATH || !raidBossBarSeen)) {
         nextAttemptTicks = -1;
         ++failedAttempts;
         ++totalChatRetries;
         statusText = useBackoff ? "Server báo lỗi liên tiếp; chuyển sang chờ ngắn rồi thử lại" : "Server báo không tìm thấy Đột Kích; thử lại ngay";
         saveStatistics();
         appendHistory("server_error_retry", selectedBossName, "backoff=" + useBackoff + ";message=" + safeSingleLine(lastChatError));
         resetAttemptState();
         ticketWaitMessageShown = false;
         if (client.field_1755 != null) {
            client.method_1507((class_437)null);
         }

         if (useBackoff) {
            scheduleNextAttempt(client, "Chưa tìm thấy Đột Kích; sẽ thử lại.");
         } else {
            beginAttempt(client);
         }
      }
   }

   private static void startLoop(class_310 client) {
      if (client.field_1724 != null && client.field_1687 != null) {
         if (loopEnabled) {
            message(client, "Đang chạy. F9 để dừng.");
         } else if (client.field_1755 != null) {
            message(client, "Đóng menu trước khi nhấn F8.");
         } else {
            loopEnabled = true;
            nextAttemptTicks = -1;
            ticketWaitMessageShown = false;
            barrierMenuRecoveryNoticeShown = false;
            consecutiveChatFailureRetries = 0;
            chatRetryPending = false;
            chatRetryUseBackoff = false;
            statusText = "Vòng lặp đã bật; đang tìm Vé Tối Thượng";
            appendHistory("loop_started", selectedBossName, "hotkey=F8");
            message(client, "RaidHelper bật. Tự tìm vé trong kho đồ; F9 để dừng.");
            beginAttempt(client);
         }
      }
   }

   private static void beginAttempt(class_310 client) {
      if (client.field_1724 != null && client.field_1687 != null) {
         if (!ensureTicketInMainHand(client)) {
            if (ticketSwapPendingSlot >= 0) {
               scheduleTicketSwapPoll();
               statusText = "Đang xác nhận chuyển Vé Tối Thượng vào hotbar";
            } else {
               if (!ticketWaitMessageShown) {
                  message(client, "Không tìm thấy Vé Tối Thượng trong hotbar/kho đồ; sẽ tự kiểm tra lại.");
                  ticketWaitMessageShown = true;
               }

               scheduleNextAttempt(client, (String)null);
               statusText = "Đang tìm Vé Tối Thượng trong kho đồ";
            }
         } else {
            ticketWaitMessageShown = false;
            reloadSkippedBosses();
            ++attemptsStarted;
            selectedBossName = "Chưa chọn";
            statusText = "Đang gửi /dk và chờ mục tham chiến";
            saveStatistics();
            appendHistory("attempt_started", selectedBossName, "command=/dk");
            phase = RaidHelperClient.Phase.WAIT_FOR_ENTRY_MENU;
            ticksWaiting = 0;
            entryScreen = null;
            entryScreenTitle = "";
            entryScreenFingerprint = "";
            bossMenuReady = false;
            ticksSinceBossMenuReady = 0;
            client.field_1724.field_3944.method_45730("dk");
         }
      }
   }

   private static boolean isTicket(class_1799 stack) {
      if (stack != null && !stack.method_7960()) {
         StringBuilder searchable = new StringBuilder(stack.method_7964().getString());
         if (stack.method_7969() != null) {
            searchable.append(' ').append(stack.method_7969().toString());
         }

         return normalize(searchable.toString()).contains("ve toi thuong");
      } else {
         return false;
      }
   }

   private static boolean ensureTicketInMainHand(class_310 client) {
      if (client != null && client.field_1724 != null && client.field_1761 != null) {
         if (client.field_1755 != null) {
            return false;
         } else {
            class_1661 inventory = client.field_1724.method_31548();
            if (ticketSwapPendingSlot >= 0) {
               if (ticketSwapPendingSlot < 9 && isTicket(inventory.method_5438(ticketSwapPendingSlot))) {
                  inventory.field_7545 = ticketSwapPendingSlot;
                  ticketHotbarSlot = ticketSwapPendingSlot;
                  ticketSwapPendingSlot = -1;
                  ticketSwapPendingTicks = 0;
                  statusText = "Đã chuyển Vé Tối Thượng lên tay chính";
                  return true;
               }

               ++ticketSwapPendingTicks;
               if (ticketSwapPendingTicks < 12) {
                  return false;
               }

               ticketSwapPendingSlot = -1;
               ticketSwapPendingTicks = 0;
            }

            if (isTicket(client.field_1724.method_6047())) {
               ticketHotbarSlot = inventory.field_7545;
               return true;
            } else {
               int destinationHotbarSlot;
               for(destinationHotbarSlot = 0; destinationHotbarSlot < 9; ++destinationHotbarSlot) {
                  if (isTicket(inventory.method_5438(destinationHotbarSlot))) {
                     inventory.field_7545 = destinationHotbarSlot;
                     ticketHotbarSlot = destinationHotbarSlot;
                     statusText = "Đã tự chọn Vé Tối Thượng từ hotbar";
                     return true;
                  }
               }

               destinationHotbarSlot = chooseHotbarDestination(inventory);

               for(int inventoryIndex = 9; inventoryIndex < 36; ++inventoryIndex) {
                  if (isTicket(inventory.method_5438(inventoryIndex))) {
                     if (swapInventorySlotToHotbar(client, inventoryIndex, destinationHotbarSlot)) {
                        inventory.field_7545 = destinationHotbarSlot;
                        if (isTicket(inventory.method_5438(destinationHotbarSlot))) {
                           ticketHotbarSlot = destinationHotbarSlot;
                           statusText = "Đã chuyển Vé Tối Thượng lên tay chính";
                           return true;
                        }

                        ticketSwapPendingSlot = destinationHotbarSlot;
                        ticketSwapPendingTicks = 0;
                     }

                     return false;
                  }
               }

               if (isTicket(client.field_1724.method_6079()) && swapOffhandToHotbar(client, destinationHotbarSlot)) {
                  inventory.field_7545 = destinationHotbarSlot;
                  if (isTicket(inventory.method_5438(destinationHotbarSlot))) {
                     ticketHotbarSlot = destinationHotbarSlot;
                     statusText = "Đã chuyển Vé Tối Thượng từ tay phụ lên tay chính";
                     return true;
                  }

                  ticketSwapPendingSlot = destinationHotbarSlot;
                  ticketSwapPendingTicks = 0;
               }

               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static int chooseHotbarDestination(class_1661 inventory) {
      if (ticketHotbarSlot >= 0 && ticketHotbarSlot < 9 && inventory.method_5438(ticketHotbarSlot).method_7960()) {
         return ticketHotbarSlot;
      } else {
         for(int i = 0; i < 9; ++i) {
            if (inventory.method_5438(i).method_7960()) {
               return i;
            }
         }

         return Math.max(0, Math.min(8, inventory.field_7545));
      }
   }

   private static boolean swapInventorySlotToHotbar(class_310 client, int inventoryIndex, int hotbarButton) {
      if (client.field_1724 != null && client.field_1761 != null && client.field_1755 == null && inventoryIndex >= 9 && inventoryIndex <= 35 && hotbarButton >= 0 && hotbarButton <= 8) {
         class_1703 handler = client.field_1724.field_7498;
         client.field_1761.method_2906(handler.field_7763, inventoryIndex, hotbarButton, class_1713.field_7791, client.field_1724);
         return true;
      } else {
         return false;
      }
   }

   private static boolean swapOffhandToHotbar(class_310 client, int hotbarButton) {
      if (client.field_1724 != null && client.field_1761 != null && client.field_1755 == null && hotbarButton >= 0 && hotbarButton <= 8) {
         class_1703 handler = client.field_1724.field_7498;
         client.field_1761.method_2906(handler.field_7763, 45, hotbarButton, class_1713.field_7791, client.field_1724);
         return true;
      } else {
         return false;
      }
   }

   private static void scheduleTicketSwapPoll() {
      phase = RaidHelperClient.Phase.IDLE;
      nextAttemptTicks = 2;
   }

   private static void stopLoop(class_310 client) {
      if (!loopEnabled && phase == RaidHelperClient.Phase.IDLE) {
         if (client.field_1724 != null) {
            message(client, "RaidHelper đang tắt.");
         }

      } else {
         loopEnabled = false;
         nextAttemptTicks = -1;
         ticketWaitMessageShown = false;
         statusText = "Đã dừng thủ công";
         appendHistory("loop_stopped", selectedBossName, "hotkey=F9");
         resetAttemptState();
         chatRetryPending = false;
         chatRetryUseBackoff = false;
         consecutiveChatFailureRetries = 0;
         message(client, "RaidHelper đã dừng.");
      }
   }

   private static void finishAttempt(class_310 client, String resultMessage) {
      if (resultMessage != null && !resultMessage.isEmpty()) {
         ++failedAttempts;
         statusText = resultMessage;
         saveStatistics();
         String var10001 = selectedBossName;
         String var10002 = phase.name();
         appendHistory("attempt_failed", var10001, "phase=" + var10002 + ";reason=" + safeSingleLine(resultMessage));
      }

      resetAttemptState();
      if (loopEnabled) {
         scheduleNextAttempt(client, resultMessage);
      } else if (resultMessage != null && !resultMessage.isEmpty()) {
         message(client, resultMessage);
      }

   }

   private static void beginBossBarWait(class_310 client, Set existingBossBars) {
      phase = RaidHelperClient.Phase.WAIT_FOR_BOSS_DEATH;
      ticksWaiting = 0;
      bossBarsBeforeSelection = new LinkedHashSet(existingBossBars);
      trackedRaidBossBars = new LinkedHashSet();
      raidBossBarSeen = false;
      ticksWaitingForBossBar = 0;
      bossBarStartNoticeShown = false;
      fightStartedAtMillis = -1L;
      statusText = "Boss đã chọn; đang chờ thanh máu xuất hiện";
   }

   private static void tickBossBarWait(class_310 client) {
      Set currentBossBars = currentBossBarIds(client);
      Set newlyVisible = new LinkedHashSet(currentBossBars);
      newlyVisible.removeAll(bossBarsBeforeSelection);
      if (!newlyVisible.isEmpty()) {
         trackedRaidBossBars.addAll(newlyVisible);
         consecutiveChatFailureRetries = 0;
         if (!raidBossBarSeen) {
            fightStartedAtMillis = System.currentTimeMillis();
            statusText = "Đang theo dõi boss";
            appendHistory("fight_started", selectedBossName, "boss_bar=visible");
         }

         raidBossBarSeen = true;
         ticksWaitingForBossBar = 0;
      } else if (!raidBossBarSeen) {
         ++ticksWaitingForBossBar;
         if (ticksWaitingForBossBar >= 1200 && !bossBarStartNoticeShown) {
            bossBarStartNoticeShown = true;
            message(client, "Chưa thấy thanh máu boss; vẫn chờ. F9 dừng.");
         }

      } else {
         boolean trackedBarStillVisible = false;
         Iterator var4 = trackedRaidBossBars.iterator();

         while(var4.hasNext()) {
            UUID trackedId = (UUID)var4.next();
            if (currentBossBars.contains(trackedId)) {
               trackedBarStillVisible = true;
               break;
            }
         }

         if (!trackedBarStillVisible) {
            finishBossFight(client);
         }
      }
   }

   private static Set currentBossBarIds(class_310 client) {
      if (client != null && client.field_1705 != null && client.field_1705.method_1740() != null) {
         Map bars = ((BossBarHudAccessor)client.field_1705.method_1740()).raidhelper$getBossBars();
         return new LinkedHashSet(bars.keySet());
      } else {
         return Collections.emptySet();
      }
   }

   private static void finishBossFight(class_310 client) {
      long durationMillis = fightStartedAtMillis > 0L ? Math.max(0L, System.currentTimeMillis() - fightStartedAtMillis) : 0L;
      if (raidBossBarSeen && durationMillis > 0L) {
         ++completedFights;
         totalFightMillis += durationMillis;
         String bossKey = selectedBossName != null && !selectedBossName.trim().isEmpty() ? selectedBossName : "Không rõ";
         completedBossCounts.put(bossKey, completedBossCounts.containsKey(bossKey) ? (Long)completedBossCounts.get(bossKey) + 1L : 1L);
         statusText = autoSkipFoughtBosses ? "Boss đã kết thúc; boss này được thêm vào danh sách đã đánh" : "Boss đã kết thúc; chuẩn bị lượt tiếp theo";
         saveStatistics();
         appendHistory("boss_completed", bossKey, "duration_ms=" + durationMillis);
      } else {
         statusText = "Thanh máu boss biến mất";
      }

      resetAttemptState();
      if (loopEnabled) {
         scheduleNextAttempt(client, (String)null, true);
      }

   }

   private static void scheduleNextAttempt(class_310 client, String resultMessage) {
      scheduleNextAttempt(client, resultMessage, false);
   }

   private static void scheduleNextAttempt(class_310 client, String resultMessage, boolean afterCompletedRaid) {
      if (!loopEnabled) {
         if (resultMessage != null && !resultMessage.isEmpty()) {
            message(client, resultMessage);
         }

      } else {
         int delayTicks;
         if (afterCompletedRaid) {
            int minTicks = completionDelayMinSeconds * 20;
            int maxTicks = completionDelayMaxSeconds * 20;
            delayTicks = ThreadLocalRandom.current().nextInt(minTicks, maxTicks + 1);
         } else {
            delayTicks = ThreadLocalRandom.current().nextInt(80, 101);
         }

         nextAttemptTicks = delayTicks;
         if (resultMessage != null && !resultMessage.isEmpty()) {
            statusText = resultMessage;
            message(client, resultMessage);
         } else if (nextAttemptTicks >= 0 && phase == RaidHelperClient.Phase.IDLE) {
            String var6;
            if (afterCompletedRaid) {
               Locale var10000 = Locale.ROOT;
               var6 = "Boss kết thúc; chờ " + String.format(var10000, "%.1f", (double)delayTicks / 20.0) + " giây";
            } else {
               var6 = "Đang chờ lượt Đột Kích tiếp theo";
            }

            statusText = var6;
         }

      }
   }

   private static void resetAttemptState() {
      phase = RaidHelperClient.Phase.IDLE;
      ticketSwapPendingSlot = -1;
      ticketSwapPendingTicks = 0;
      ticksWaiting = 0;
      entryScreen = null;
      entryScreenTitle = "";
      entryScreenFingerprint = "";
      bossMenuReady = false;
      ticksSinceBossMenuReady = 0;
      bossBarsBeforeSelection = new LinkedHashSet();
      trackedRaidBossBars = new LinkedHashSet();
      raidBossBarSeen = false;
      ticksWaitingForBossBar = 0;
      bossBarStartNoticeShown = false;
      fightStartedAtMillis = -1L;
   }

   private static int findWitherSkullSlot(class_465 screen) {
      List slots = screen.method_17577().field_7761;
      int guiCount = Math.max(0, slots.size() - 36);

      for(int i = 0; i < guiCount; ++i) {
         class_1735 slot = (class_1735)slots.get(i);
         class_1799 stack = slot.method_7677();
         if (!stack.method_7960()) {
            String name = normalize(stack.method_7964().getString());
            if (stack.method_7909() == class_1802.field_8791 || name.contains("wither skeleton skull") || name.contains("dau lau wither")) {
               return slot.field_7874;
            }
         }
      }

      return -1;
   }

   private static int findJoinLabelSlot(class_465 screen) {
      List slots = screen.method_17577().field_7761;
      int guiCount = Math.max(0, slots.size() - 36);

      for(int i = 0; i < guiCount; ++i) {
         class_1735 slot = (class_1735)slots.get(i);
         class_1799 stack = slot.method_7677();
         if (!stack.method_7960() && stack.method_7909() != class_1802.field_8077) {
            String name = normalize(stack.method_7964().getString());
            if (name.contains("tham chien") || name.contains("tham gia")) {
               return slot.field_7874;
            }
         }
      }

      return -1;
   }

   private static String getBossNameForSlot(class_465 screen, int slotId) {
      Iterator var2 = screen.method_17577().field_7761.iterator();

      while(var2.hasNext()) {
         class_1735 slot = (class_1735)var2.next();
         if (slot.field_7874 == slotId) {
            class_1799 stack = slot.method_7677();
            if (!stack.method_7960()) {
               String name = stack.method_7964().getString();
               return name != null && !name.trim().isEmpty() ? name.trim() : "Boss ô " + slotId;
            }
         }
      }

      return "Boss ô " + slotId;
   }

   private static boolean isBarrierOnlyBossMenu(class_465 screen) {
      if (screen == null) {
         return false;
      } else {
         List slots = screen.method_17577().field_7761;
         int guiCount = Math.max(0, slots.size() - 36);
         int barrierCount = 0;

         for(int i = 0; i < guiCount; ++i) {
            class_1799 stack = ((class_1735)slots.get(i)).method_7677();
            if (!stack.method_7960()) {
               if (stack.method_7909() != class_1802.field_8077) {
                  return false;
               }

               ++barrierCount;
            }
         }

         return barrierCount > 0;
      }
   }

   private static void recoverFromBarrierOnlyMenu(class_310 client, class_465 screen) {
      if (client.field_1755 == screen) {
         client.method_1507((class_437)null);
      }

      ++failedAttempts;
      saveStatistics();
      appendHistory("barrier_menu_recovery", selectedBossName, "action=closed_barrier_only_menu_and_resumed_retry_loop");
      resetAttemptState();
      ticketWaitMessageShown = false;
      String notice = null;
      if (!barrierMenuRecoveryNoticeShown) {
         notice = "Menu boss chỉ có Barrier; đã thoát menu và sẽ thử lại vòng Đột Kích.";
         barrierMenuRecoveryNoticeShown = true;
      }

      scheduleNextAttempt(client, notice);
   }

   private static List findReadyBossSlots(class_465 screen) {
      List candidates = new ArrayList();
      List slots = screen.method_17577().field_7761;
      int guiCount = Math.max(0, slots.size() - 36);

      for(int i = 0; i < guiCount; ++i) {
         class_1735 slot = (class_1735)slots.get(i);
         class_1799 stack = slot.method_7677();
         if (!stack.method_7960() && stack.method_7909() != class_1802.field_8077) {
            String name = normalize(stack.method_7964().getString());
            boolean pumpkin = stack.method_7909() == class_1802.field_17518 || stack.method_7909() == class_1802.field_17519 || name.contains("pumpkin") || name.contains("bi ngo");
            if (pumpkin && findSkipMatch(stack) == null && findFoughtBossMatch(stack) == null) {
               candidates.add(slot.field_7874);
            }
         }
      }

      return candidates;
   }

   private static void renderHud(class_332 context, float tickDelta) {
      if (hudVisible) {
         class_310 client = class_310.method_1551();
         if (client != null && client.field_1724 != null && client.field_1687 != null && client.field_1772 != null) {
            List lines = new ArrayList();
            lines.add("RaidHelper v2.2.6  |  F6 Ẩn/hiện");
            String var10001 = loopEnabled ? "ĐANG CHẠY" : "ĐÃ DỪNG";
            lines.add("Chế độ: " + var10001);
            var10001 = phaseLabel();
            lines.add("Tiến trình: " + var10001);
            var10001 = selectedBossName == null ? "Chưa chọn" : selectedBossName;
            lines.add("Boss: " + var10001);
            var10001 = countdownLabel();
            lines.add("Đếm ngược: " + var10001);
            var10001 = getBossSelectionModeLabel();
            lines.add("Cách chọn boss: " + var10001);
            var10001 = completionDelayRangeLabel();
            lines.add("Chờ sau boss: " + var10001);
            var10001 = autoSkipFoughtBosses ? "BẬT" : "TẮT";
            lines.add("Tự bỏ qua boss đã đánh: " + var10001);
            long var17 = attemptsStarted;
            lines.add("Lượt bắt đầu: " + var17 + " | Boss hạ: " + completedFights);
            var17 = failedAttempts;
            lines.add("Lỗi: " + var17 + " | Chọn boss: " + bossSelections + " | Thử lại: " + totalChatRetries);
            var10001 = mostCompletedBossLabel();
            lines.add("Boss hay hoàn thành: " + var10001);
            var10001 = averageFightDurationLabel();
            lines.add("Trận trung bình: " + var10001);
            lines.add("Trạng thái: " + (statusText == null ? "Sẵn sàng" : statusText));
            int screenWidth = client.method_22683().method_4486();
            int maxTextWidth = Math.max(120, Math.min(360, screenWidth - 28));
            int panelWidth = 0;
            List visibleLines = new ArrayList();

            String trimmed;
            for(Iterator var8 = lines.iterator(); var8.hasNext(); panelWidth = Math.max(panelWidth, client.field_1772.method_1727(trimmed))) {
               String line = (String)var8.next();
               trimmed = trimToPixelWidth(client, line, maxTextWidth);
               visibleLines.add(trimmed);
            }

            panelWidth += 14;
            int x = 7;
            int y = 7;
            int lineHeight = 11;
            int panelHeight = 8 + visibleLines.size() * lineHeight;
            context.method_25294(x, y, x + panelWidth, y + panelHeight, -1441786595);
            context.method_25294(x, y, x + panelWidth, y + 2, loopEnabled ? -12404349 : -8947849);

            for(int i = 0; i < visibleLines.size(); ++i) {
               int color = i == 0 ? -7477576 : -1512720;
               if (i == 1) {
                  color = loopEnabled ? -8519782 : -15241;
               }

               context.method_25303(client.field_1772, (String)visibleLines.get(i), x + 7, y + 5 + i * lineHeight, color);
            }

         }
      }
   }

   private static String phaseLabel() {
      if (!loopEnabled) {
         return "Đã dừng";
      } else {
         switch (phase) {
            case WAIT_FOR_ENTRY_MENU:
               return "Mở /dk, chờ mục tham chiến";
            case WAIT_FOR_BOSS_MENU:
               return bossMenuReady ? "Đang chọn boss ngẫu nhiên" : "Chờ danh sách boss";
            case WAIT_FOR_BOSS_DEATH:
               return raidBossBarSeen ? "Đang theo dõi thanh máu boss" : "Chờ thanh máu boss xuất hiện";
            case IDLE:
            default:
               return nextAttemptTicks >= 0 ? "Chờ lượt tiếp theo" : "Đang chuẩn bị";
         }
      }
   }

   private static String countdownLabel() {
      return loopEnabled && phase == RaidHelperClient.Phase.IDLE && nextAttemptTicks >= 0 ? String.format(Locale.ROOT, "%.1f giây", (double)Math.max(0, nextAttemptTicks) / 20.0) : "—";
   }

   private static String mostCompletedBossLabel() {
      String bestName = null;
      long bestCount = 0L;
      Iterator var3 = completedBossCounts.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry entry = (Map.Entry)var3.next();
         if (entry.getValue() != null && (Long)entry.getValue() > bestCount) {
            bestName = (String)entry.getKey();
            bestCount = (Long)entry.getValue();
         }
      }

      return bestName == null ? "chưa có dữ liệu" : bestName + " × " + bestCount;
   }

   private static String completionDelayRangeLabel() {
      return completionDelayMinSeconds + "–" + completionDelayMaxSeconds + " giây";
   }

   private static String averageFightDurationLabel() {
      if (completedFights <= 0L) {
         return "chưa có dữ liệu";
      } else {
         double seconds = (double)totalFightMillis / (double)completedFights / 1000.0;
         return String.format(Locale.ROOT, "%.1f giây (%d trận)", seconds, completedFights);
      }
   }

   private static String trimToPixelWidth(class_310 client, String value, int maxWidth) {
      if (client.field_1772.method_1727(value) <= maxWidth) {
         return value;
      } else {
         String result;
         for(result = value; result.length() > 1 && client.field_1772.method_1727(result + "…") > maxWidth; result = result.substring(0, result.length() - 1)) {
         }

         return result + "…";
      }
   }

   private static void loadDelaySettings() {
      try {
         Path configDir = FabricLoader.getInstance().getConfigDir();
         Files.createDirectories(configDir);
         settingsPath = configDir.resolve("raidhelper-settings.properties");
         Properties properties = new Properties();
         if (Files.exists(settingsPath, new LinkOption[0])) {
            Reader reader = Files.newBufferedReader(settingsPath, StandardCharsets.UTF_8);

            try {
               properties.load(reader);
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }
         }

         int min = parseBoundedInt(properties.getProperty("completionDelayMinSeconds"), 4, 1, 300);
         int max = parseBoundedInt(properties.getProperty("completionDelayMaxSeconds"), 5, 1, 300);
         if (min > max) {
            min = 4;
            max = 5;
         }

         completionDelayMinSeconds = min;
         completionDelayMaxSeconds = max;
         autoSkipFoughtBosses = Boolean.parseBoolean(properties.getProperty("autoSkipFoughtBosses", "true"));
         bossSelectionMode = parseBossSelectionMode(properties.getProperty("bossSelectionMode", "RANDOM"));
         writeDelaySettings(min, max);
      } catch (IOException var7) {
         System.err.println("[RaidHelper] Could not load delay settings: " + var7.getMessage());
         completionDelayMinSeconds = 4;
         completionDelayMaxSeconds = 5;
      }

   }

   private static int parseBoundedInt(String value, int fallback, int min, int max) {
      if (value == null) {
         return fallback;
      } else {
         try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
         } catch (NumberFormatException var5) {
            return fallback;
         }
      }
   }

   private static boolean writeDelaySettings(int minSeconds, int maxSeconds) throws IOException {
      if (settingsPath == null) {
         Path configDir = FabricLoader.getInstance().getConfigDir();
         Files.createDirectories(configDir);
         settingsPath = configDir.resolve("raidhelper-settings.properties");
      }

      Properties properties = new Properties();
      properties.setProperty("completionDelayMinSeconds", Integer.toString(minSeconds));
      properties.setProperty("completionDelayMaxSeconds", Integer.toString(maxSeconds));
      properties.setProperty("autoSkipFoughtBosses", Boolean.toString(autoSkipFoughtBosses));
      properties.setProperty("bossSelectionMode", bossSelectionMode.name());
      Writer writer = Files.newBufferedWriter(settingsPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

      try {
         properties.store(writer, "RaidHelper post-raid delay settings; edit in game from F7");
      } catch (Throwable var7) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (writer != null) {
         writer.close();
      }

      return true;
   }

   private static BossSelectionMode parseBossSelectionMode(String value) {
      if (value == null) {
         return RaidHelperClient.BossSelectionMode.RANDOM;
      } else {
         String normalized = value.trim().toUpperCase(Locale.ROOT);
         if (!"LEFT_TO_RIGHT".equals(normalized) && !"TOP_TO_BOTTOM".equals(normalized) && !"SEQUENTIAL".equals(normalized)) {
            return "RANDOM".equals(normalized) ? RaidHelperClient.BossSelectionMode.RANDOM : RaidHelperClient.BossSelectionMode.RANDOM;
         } else {
            return RaidHelperClient.BossSelectionMode.SEQUENTIAL;
         }
      }
   }

   static String getBossSelectionModeLabel() {
      switch (bossSelectionMode) {
         case SEQUENTIAL:
            return "Tuần tự theo hàng";
         case RANDOM:
         default:
            return "Ngẫu nhiên";
      }
   }

   static String cycleBossSelectionMode() {
      BossSelectionMode previous = bossSelectionMode;
      bossSelectionMode = bossSelectionMode == RaidHelperClient.BossSelectionMode.RANDOM ? RaidHelperClient.BossSelectionMode.SEQUENTIAL : RaidHelperClient.BossSelectionMode.RANDOM;

      try {
         writeDelaySettings(completionDelayMinSeconds, completionDelayMaxSeconds);
         resetOrderedSelectionPosition();
         String result = "Đã lưu cách chọn boss: " + getBossSelectionModeLabel();
         statusText = result;
         appendHistory("boss_selection_mode_changed", "", "mode=" + bossSelectionMode.name());
         return result;
      } catch (IOException var2) {
         bossSelectionMode = previous;
         System.err.println("[RaidHelper] Could not save boss-selection mode: " + var2.getMessage());
         return "Không lưu được cách chọn boss; hãy kiểm tra thư mục config.";
      }
   }

   private static int chooseBossSlot(class_465 screen, List readySlots) {
      if (readySlots != null && !readySlots.isEmpty()) {
         if (bossSelectionMode == RaidHelperClient.BossSelectionMode.RANDOM) {
            return (Integer)readySlots.get(ThreadLocalRandom.current().nextInt(readySlots.size()));
         } else {
            List candidates = new ArrayList();
            Set wanted = new LinkedHashSet(readySlots);
            Iterator var4 = screen.method_17577().field_7761.iterator();

            while(var4.hasNext()) {
               class_1735 slot = (class_1735)var4.next();
               if (wanted.contains(slot.field_7874)) {
                  candidates.add(slot);
               }
            }

            if (candidates.isEmpty()) {
               return (Integer)readySlots.get(ThreadLocalRandom.current().nextInt(readySlots.size()));
            } else {
               candidates.sort(Comparator.comparingInt((slotx) -> {
                  return slotx.field_7872;
               }).thenComparingInt((slotx) -> {
                  return slotx.field_7873;
               }).thenComparingInt((slotx) -> {
                  return slotx.field_7874;
               }));
               class_1735 chosen = null;
               if (lastOrderedBossSlotX >= 0 && lastOrderedBossSlotY >= 0) {
                  Iterator var8 = candidates.iterator();

                  while(var8.hasNext()) {
                     class_1735 candidate = (class_1735)var8.next();
                     if (isAfterLastOrderedPosition(candidate)) {
                        chosen = candidate;
                        break;
                     }
                  }
               }

               if (chosen == null) {
                  chosen = (class_1735)candidates.get(0);
               }

               lastOrderedBossSlotX = chosen.field_7873;
               lastOrderedBossSlotY = chosen.field_7872;
               lastOrderedBossSlotId = chosen.field_7874;
               return chosen.field_7874;
            }
         }
      } else {
         return -1;
      }
   }

   private static boolean isAfterLastOrderedPosition(class_1735 slot) {
      if (slot.field_7872 != lastOrderedBossSlotY) {
         return slot.field_7872 > lastOrderedBossSlotY;
      } else if (slot.field_7873 != lastOrderedBossSlotX) {
         return slot.field_7873 > lastOrderedBossSlotX;
      } else {
         return slot.field_7874 > lastOrderedBossSlotId;
      }
   }

   private static void resetOrderedSelectionPosition() {
      lastOrderedBossSlotX = -1;
      lastOrderedBossSlotY = -1;
      lastOrderedBossSlotId = -1;
   }

   static int getCompletionDelayMinSeconds() {
      return completionDelayMinSeconds;
   }

   static int getCompletionDelayMaxSeconds() {
      return completionDelayMaxSeconds;
   }

   static String saveCompletionDelaySettings(String minText, String maxText) {
      int min;
      int max;
      try {
         min = Integer.parseInt(minText == null ? "" : minText.trim());
         max = Integer.parseInt(maxText == null ? "" : maxText.trim());
      } catch (NumberFormatException var6) {
         return "Nhập số nguyên giây cho cả hai ô.";
      }

      if (min >= 1 && max >= 1 && min <= 300 && max <= 300) {
         if (min > max) {
            return "Thời gian tối thiểu không được lớn hơn tối đa.";
         } else {
            try {
               writeDelaySettings(min, max);
               completionDelayMinSeconds = min;
               completionDelayMaxSeconds = max;
               statusText = "Đã lưu khoảng thời gian chờ sau boss";
               appendHistory("settings_changed", "", "min_seconds=" + min + ";max_seconds=" + max);
               return null;
            } catch (IOException var5) {
               System.err.println("[RaidHelper] Could not save settings: " + var5.getMessage());
               return "Không lưu được cấu hình; hãy kiểm tra thư mục config.";
            }
         }
      } else {
         return "Thời gian phải từ 1 đến 300 giây.";
      }
   }

   static boolean isAutoSkipFoughtBosses() {
      return autoSkipFoughtBosses;
   }

   static String toggleAutoSkipFoughtBosses() {
      boolean previous = autoSkipFoughtBosses;
      autoSkipFoughtBosses = !autoSkipFoughtBosses;

      try {
         writeDelaySettings(completionDelayMinSeconds, completionDelayMaxSeconds);
         String result = autoSkipFoughtBosses ? "Tự bỏ qua boss đã đánh: BẬT" : "Tự bỏ qua boss đã đánh: TẮT";
         appendHistory("fought_boss_auto_skip_changed", "", "enabled=" + autoSkipFoughtBosses);
         statusText = result;
         return result;
      } catch (IOException var2) {
         autoSkipFoughtBosses = previous;
         System.err.println("[RaidHelper] Could not save auto-skip setting: " + var2.getMessage());
         return "Không lưu được cài đặt.";
      }
   }

   static List getFoughtBossEntriesForScreen() {
      List result = new ArrayList();
      Iterator var1 = completedBossCounts.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry entry = (Map.Entry)var1.next();
         result.add(new FoughtBossEntry((String)entry.getKey(), entry.getValue() == null ? 0L : (Long)entry.getValue()));
      }

      result.sort((a, b) -> {
         return Long.compare(b.count, a.count);
      });
      return result;
   }

   static boolean resetAllStatistics() {
      attemptsStarted = 0L;
      bossSelections = 0L;
      completedFights = 0L;
      failedAttempts = 0L;
      totalChatRetries = 0L;
      totalFightMillis = 0L;
      fightStartedAtMillis = -1L;
      completedBossCounts.clear();
      resetOrderedSelectionPosition();
      selectedBossName = "Chưa chọn";
      consecutiveChatFailureRetries = 0;
      chatRetryPending = false;
      chatRetryUseBackoff = false;
      lastChatError = "";
      nextAttemptTicks = -1;
      resetAttemptState();
      saveStatistics();
      boolean historyReset = resetHistoryFile();
      statusText = historyReset ? "Đã reset toàn bộ thống kê, danh sách boss và lịch sử" : "Đã reset bộ đếm; không thể xóa file lịch sử CSV";
      return historyReset;
   }

   private static boolean resetHistoryFile() {
      try {
         if (historyPath == null) {
            historyPath = FabricLoader.getInstance().getConfigDir().resolve("raidhelper-history.csv");
         }

         Files.createDirectories(historyPath.getParent());
         Files.write(historyPath, Collections.singletonList("timestamp,event,boss,details"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
         return true;
      } catch (IOException var1) {
         System.err.println("[RaidHelper] Could not reset event history: " + var1.getMessage());
         return false;
      }
   }

   static boolean resetFoughtBossList() {
      return resetAllStatistics();
   }

   static String resetCompletionDelaySettings() {
      return saveCompletionDelaySettings(Integer.toString(4), Integer.toString(5));
   }

   private static void loadStatistics() {
      try {
         Path configDir = FabricLoader.getInstance().getConfigDir();
         Files.createDirectories(configDir);
         statsPath = configDir.resolve("raidhelper-stats.properties");
         historyPath = configDir.resolve("raidhelper-history.csv");
         if (Files.exists(statsPath, new LinkOption[0])) {
            Properties properties = new Properties();
            Reader reader = Files.newBufferedReader(statsPath, StandardCharsets.UTF_8);

            try {
               properties.load(reader);
            } catch (Throwable var8) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (reader != null) {
               reader.close();
            }

            attemptsStarted = parseLong(properties.getProperty("attemptsStarted"), 0L);
            bossSelections = parseLong(properties.getProperty("bossSelections"), 0L);
            completedFights = parseLong(properties.getProperty("completedFights"), 0L);
            failedAttempts = parseLong(properties.getProperty("failedAttempts"), 0L);
            totalChatRetries = parseLong(properties.getProperty("totalChatRetries"), 0L);
            totalFightMillis = parseLong(properties.getProperty("totalFightMillis"), 0L);
            int entries = (int)Math.max(0L, Math.min(10000L, parseLong(properties.getProperty("bossEntries"), 0L)));

            for(int i = 0; i < entries; ++i) {
               String name = properties.getProperty("bossName." + i);
               long count = parseLong(properties.getProperty("bossCompleted." + i), 0L);
               if (name != null && !name.trim().isEmpty() && count > 0L) {
                  completedBossCounts.put(name, count);
               }
            }
         }

         if (!Files.exists(historyPath, new LinkOption[0]) || Files.size(historyPath) == 0L) {
            Files.write(historyPath, Collections.singletonList("timestamp,event,boss,details"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         }
      } catch (IOException var9) {
         System.err.println("[RaidHelper] Could not load statistics: " + var9.getMessage());
         statusText = "Không đọc được file thống kê";
      }

   }

   private static long parseLong(String value, long fallback) {
      if (value == null) {
         return fallback;
      } else {
         try {
            return Long.parseLong(value.trim());
         } catch (NumberFormatException var4) {
            return fallback;
         }
      }
   }

   private static void saveStatistics() {
      if (statsPath == null) {
         loadStatistics();
         if (statsPath == null) {
            return;
         }
      }

      Properties properties = new Properties();
      properties.setProperty("attemptsStarted", Long.toString(attemptsStarted));
      properties.setProperty("bossSelections", Long.toString(bossSelections));
      properties.setProperty("completedFights", Long.toString(completedFights));
      properties.setProperty("failedAttempts", Long.toString(failedAttempts));
      properties.setProperty("totalChatRetries", Long.toString(totalChatRetries));
      properties.setProperty("totalFightMillis", Long.toString(totalFightMillis));
      properties.setProperty("bossEntries", Integer.toString(completedBossCounts.size()));
      int i = 0;

      for(Iterator var2 = completedBossCounts.entrySet().iterator(); var2.hasNext(); ++i) {
         Map.Entry entry = (Map.Entry)var2.next();
         properties.setProperty("bossName." + i, (String)entry.getKey());
         properties.setProperty("bossCompleted." + i, Long.toString((Long)entry.getValue()));
      }

      try {
         Writer writer = Files.newBufferedWriter(statsPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

         try {
            properties.store(writer, "RaidHelper persistent statistics; generated automatically");
         } catch (Throwable var6) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException var7) {
         System.err.println("[RaidHelper] Could not save statistics: " + var7.getMessage());
      }

   }

   private static void appendHistory(String event, String boss, String details) {
      if (historyPath == null) {
         Path configDir = FabricLoader.getInstance().getConfigDir();
         historyPath = configDir.resolve("raidhelper-history.csv");
      }

      try {
         Files.createDirectories(historyPath.getParent());
         if (!Files.exists(historyPath, new LinkOption[0]) || Files.size(historyPath) == 0L) {
            Files.write(historyPath, Collections.singletonList("timestamp,event,boss,details"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         }

         String var10000 = csv(LocalDateTime.now().format(HISTORY_TIME));
         String row = var10000 + "," + csv(event) + "," + csv(boss == null ? "" : boss) + "," + csv(details == null ? "" : details);
         Files.write(historyPath, Collections.singletonList(row), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException var4) {
         System.err.println("[RaidHelper] Could not append history: " + var4.getMessage());
      }

   }

   private static String csv(String value) {
      String safe = value == null ? "" : value.replace("\r", " ").replace("\n", " ").replace("\"", "\"\"");
      return "\"" + safe + "\"";
   }

   private static String safeSingleLine(String value) {
      return value == null ? "" : value.replace("\r", " ").replace("\n", " ");
   }

   private static void ensureSkipListFile() {
      try {
         Path configDir = FabricLoader.getInstance().getConfigDir();
         Files.createDirectories(configDir);
         skipBossesPath = configDir.resolve("raidhelper-skip-bosses.txt");
         if (!Files.exists(skipBossesPath, new LinkOption[0])) {
            List template = new ArrayList();
            template.add("# RaidHelper boss skip list");
            template.add("# Add one boss name or distinctive keyword per line.");
            template.add("# Boss entries whose visible name or item NBT/lore contains a term below will be skipped.");
            template.add("# Matching ignores capitalization and Vietnamese accents; matching is partial.");
            template.add("# Example: remove the # and replace this with a boss name from your server.");
            template.add("# Ten Boss Qua Manh");
            template.add("");
            Files.write(skipBossesPath, template, StandardCharsets.UTF_8);
         }
      } catch (IOException var2) {
         System.err.println("[RaidHelper] Could not create raidhelper-skip-bosses.txt: " + var2.getMessage());
      }

   }

   static List getBossRulesForScreen() {
      ensureSkipListFile();
      return readBossRules();
   }

   static boolean addBossRule(String name) {
      String clean = name == null ? "" : name.trim();
      String normalized = normalize(clean);
      if (normalized.length() < 2) {
         return false;
      } else {
         List rules = readBossRules();

         for(int i = 0; i < rules.size(); ++i) {
            BossRule rule = (BossRule)rules.get(i);
            if (normalize(rule.name).equals(normalized)) {
               if (rule.skipped) {
                  return false;
               }

               rules.set(i, new BossRule(rule.name, true));
               return saveBossRules(rules);
            }
         }

         rules.add(new BossRule(clean, true));
         return saveBossRules(rules);
      }
   }

   static boolean toggleBossRule(String name) {
      String normalized = normalize(name);
      List rules = readBossRules();

      for(int i = 0; i < rules.size(); ++i) {
         BossRule rule = (BossRule)rules.get(i);
         if (normalize(rule.name).equals(normalized)) {
            rules.set(i, new BossRule(rule.name, !rule.skipped));
            return saveBossRules(rules);
         }
      }

      return false;
   }

   static boolean removeBossRule(String name) {
      String normalized = normalize(name);
      List rules = readBossRules();
      boolean removed = false;

      for(int i = rules.size() - 1; i >= 0; --i) {
         if (normalize(((BossRule)rules.get(i)).name).equals(normalized)) {
            rules.remove(i);
            removed = true;
         }
      }

      return removed && saveBossRules(rules);
   }

   private static List readBossRules() {
      List result = new ArrayList();
      if (skipBossesPath != null && Files.exists(skipBossesPath, new LinkOption[0])) {
         Map unique = new LinkedHashMap();

         try {
            Iterator var2 = Files.readAllLines(skipBossesPath, StandardCharsets.UTF_8).iterator();

            while(var2.hasNext()) {
               String line = (String)var2.next();
               String trimmed = line.trim();
               if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                  boolean skipped = true;
                  String displayName = trimmed;
                  String lower = trimmed.toLowerCase(Locale.ROOT);
                  if (lower.startsWith("skip:")) {
                     displayName = trimmed.substring(5).trim();
                  } else if (lower.startsWith("allow:")) {
                     skipped = false;
                     displayName = trimmed.substring(6).trim();
                  }

                  String normalized = normalize(displayName);
                  if (normalized.length() >= 2) {
                     unique.put(normalized, new BossRule(displayName, skipped));
                  }
               }
            }
         } catch (IOException var9) {
            System.err.println("[RaidHelper] Could not read raidhelper-skip-bosses.txt: " + var9.getMessage());
         }

         result.addAll(unique.values());
         return result;
      } else {
         return result;
      }
   }

   private static boolean saveBossRules(List rules) {
      if (skipBossesPath == null) {
         ensureSkipListFile();
      }

      if (skipBossesPath == null) {
         return false;
      } else {
         List lines = new ArrayList();
         lines.add("# RaidHelper boss filter. Edit in game with F7.");
         lines.add("# Click a row in game to toggle it. Red = skipped; green = allowed.");
         lines.add("# Format: skip: Boss Name  OR  allow: Boss Name");
         Iterator var2 = rules.iterator();

         while(var2.hasNext()) {
            BossRule rule = (BossRule)var2.next();
            String var10001 = rule.skipped ? "skip: " : "allow: ";
            lines.add(var10001 + rule.name);
         }

         try {
            Files.write(skipBossesPath, lines, StandardCharsets.UTF_8);
            reloadSkippedBosses();
            return true;
         } catch (IOException var4) {
            System.err.println("[RaidHelper] Could not save raidhelper-skip-bosses.txt: " + var4.getMessage());
            return false;
         }
      }
   }

   private static void reloadSkippedBosses() {
      if (skipBossesPath == null) {
         ensureSkipListFile();
      }

      Set loaded = new LinkedHashSet();
      Iterator var1 = readBossRules().iterator();

      while(var1.hasNext()) {
         BossRule rule = (BossRule)var1.next();
         if (rule.skipped) {
            String term = normalize(rule.name);
            if (term.length() >= 2) {
               loaded.add(term);
            }
         }
      }

      skippedBossTerms = Collections.unmodifiableSet(loaded);
   }

   private static String findSkipMatch(class_1799 stack) {
      StringBuilder searchable = new StringBuilder(stack.method_7964().getString());
      if (stack.method_7969() != null) {
         searchable.append(' ').append(stack.method_7969().toString());
      }

      String normalizedSearchable = normalize(searchable.toString());
      Iterator var3 = skippedBossTerms.iterator();

      String term;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         term = (String)var3.next();
      } while(!normalizedSearchable.contains(term));

      return term;
   }

   private static String findFoughtBossMatch(class_1799 stack) {
      if (autoSkipFoughtBosses && stack != null && !stack.method_7960() && !completedBossCounts.isEmpty()) {
         String visibleName = normalize(stack.method_7964().getString());
         if (visibleName.isEmpty()) {
            return null;
         } else {
            Iterator var2 = completedBossCounts.keySet().iterator();

            String foughtName;
            String normalizedFought;
            do {
               do {
                  if (!var2.hasNext()) {
                     return null;
                  }

                  foughtName = (String)var2.next();
                  normalizedFought = normalize(foughtName);
               } while(normalizedFought.length() < 3);
            } while(!visibleName.equals(normalizedFought) && !visibleName.startsWith(normalizedFought + " ") && !normalizedFought.startsWith(visibleName + " "));

            return foughtName;
         }
      } else {
         return null;
      }
   }

   private static String menuFingerprint(class_465 screen) {
      StringBuilder result = new StringBuilder(normalize(screen.method_25440().getString()));
      List slots = screen.method_17577().field_7761;
      int guiCount = Math.max(0, slots.size() - 36);

      for(int i = 0; i < guiCount; ++i) {
         class_1799 stack = ((class_1735)slots.get(i)).method_7677();
         result.append('|').append(i).append(':');
         if (!stack.method_7960()) {
            result.append(stack.method_7909()).append('/');
            result.append(normalize(stack.method_7964().getString())).append('/');
            result.append(stack.method_7947());
            if (stack.method_7969() != null) {
               result.append('/').append(stack.method_7969().toString());
            }
         } else {
            result.append("empty");
         }
      }

      return result.toString();
   }

   private static void clickSlot(class_310 client, class_465 screen, int slotId) {
      if (client.field_1761 != null && client.field_1724 != null) {
         class_1703 handler = screen.method_17577();
         client.field_1761.method_2906(handler.field_7763, slotId, 0, class_1713.field_7790, client.field_1724);
      }
   }

   private static void stop(class_310 client, String text) {
      phase = RaidHelperClient.Phase.IDLE;
      ticksWaiting = 0;
      entryScreen = null;
      entryScreenTitle = "";
      entryScreenFingerprint = "";
      bossMenuReady = false;
      message(client, text);
   }

   private static void message(class_310 client, String text) {
      if (client.field_1705 != null) {
         client.field_1705.method_1743().method_1812(class_2561.method_43470("[RaidHelper] " + text));
      }

   }

   private static String normalize(String input) {
      String noAccents = Normalizer.normalize(input == null ? "" : input, Form.NFD).replaceAll("\\p{M}+", "");
      return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]", " ").replaceAll("\\s+", " ").trim();
   }

   static {
      bossSelectionMode = RaidHelperClient.BossSelectionMode.RANDOM;
      lastOrderedBossSlotX = -1;
      lastOrderedBossSlotY = -1;
      lastOrderedBossSlotId = -1;
      statusText = "Sẵn sàng — F8 bắt đầu";
      selectedBossName = "Chưa chọn";
      attemptsStarted = 0L;
      bossSelections = 0L;
      completedFights = 0L;
      failedAttempts = 0L;
      totalChatRetries = 0L;
      totalFightMillis = 0L;
      fightStartedAtMillis = -1L;
      completedBossCounts = new LinkedHashMap();
      phase = RaidHelperClient.Phase.IDLE;
      ticksWaiting = 0;
      entryScreenTitle = "";
      entryScreenFingerprint = "";
      bossMenuReady = false;
      ticksSinceBossMenuReady = 0;
      bossBarsBeforeSelection = new LinkedHashSet();
      trackedRaidBossBars = new LinkedHashSet();
      raidBossBarSeen = false;
      ticksWaitingForBossBar = 0;
      bossBarStartNoticeShown = false;
   }

   private static enum Phase {
      IDLE,
      WAIT_FOR_ENTRY_MENU,
      WAIT_FOR_BOSS_MENU,
      WAIT_FOR_BOSS_DEATH;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{IDLE, WAIT_FOR_ENTRY_MENU, WAIT_FOR_BOSS_MENU, WAIT_FOR_BOSS_DEATH};
      }
   }

   static enum BossSelectionMode {
      SEQUENTIAL,
      RANDOM;

      // $FF: synthetic method
      private static BossSelectionMode[] $values() {
         return new BossSelectionMode[]{SEQUENTIAL, RANDOM};
      }
   }

   static final class FoughtBossEntry {
      final String name;
      final long count;

      FoughtBossEntry(String name, long count) {
         this.name = name;
         this.count = count;
      }
   }

   static final class BossRule {
      final String name;
      final boolean skipped;

      BossRule(String name, boolean skipped) {
         this.name = name;
         this.skipped = skipped;
      }
   }
}
