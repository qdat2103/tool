package com.raidhub.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public final class RaidHelperSkipScreen extends class_437 {
   private static final int ROW_HEIGHT = 23;
   private final class_437 parent;
   private class_342 nameField;
   private List rules = new ArrayList();
   private int listLeft;
   private int listRight;
   private int listTop;
   private int listBottom;
   private int visibleRows;
   private int scrollOffset;
   private String status = "Nhấn vào một dòng để bật/tắt bỏ qua; nhấn X để xóa.";

   public RaidHelperSkipScreen(class_437 parent) {
      super(class_2561.method_43470("RaidHelper - Lọc boss Đột Kích"));
      this.parent = parent;
   }

   protected void method_25426() {
      this.listLeft = Math.max(12, this.field_22789 / 2 - 165);
      this.listRight = Math.min(this.field_22789 - 12, this.field_22789 / 2 + 165);
      this.listTop = 91;
      this.listBottom = Math.max(this.listTop + 23, this.field_22790 - 66);
      this.visibleRows = Math.max(1, (this.listBottom - this.listTop) / 23);
      this.nameField = new class_342(this.field_22793, this.listLeft, 47, Math.max(120, this.listRight - this.listLeft - 88), 20, class_2561.method_43470(""));
      this.nameField.method_1880(80);
      this.nameField.method_1887("Tên boss hoặc từ khóa...");
      this.method_37063(this.nameField);
      this.method_48265(this.nameField);
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Thêm"), (button) -> {
         this.addRule();
      }).method_46434(this.listRight - 80, 47, 80, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Boss đã đánh"), (button) -> {
         if (this.field_22787 != null) {
            this.field_22787.method_1507(new RaidHelperFoughtBossScreen(this));
         }

      }).method_46434(this.field_22789 / 2 - 165, this.field_22790 - 29, 96, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Cài đặt"), (button) -> {
         if (this.field_22787 != null) {
            this.field_22787.method_1507(new RaidHelperSettingsScreen(this));
         }

      }).method_46434(this.field_22789 / 2 - 63, this.field_22790 - 29, 110, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Đóng"), (button) -> {
         this.method_25419();
      }).method_46434(this.field_22789 / 2 + 55, this.field_22790 - 29, 85, 20).method_46431());
      this.refreshRules();
   }

   private void refreshRules() {
      this.rules = RaidHelperClient.getBossRulesForScreen();
      int maxOffset = Math.max(0, this.rules.size() - this.visibleRows);
      this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));
   }

   private void addRule() {
      String value = this.nameField.method_1882().trim();
      if (value.isEmpty()) {
         this.status = "Nhập tên boss hoặc một từ khóa trước khi thêm.";
      } else {
         boolean saved = RaidHelperClient.addBossRule(value);
         if (saved) {
            this.status = "Đã thêm/kích hoạt: " + value;
            this.nameField.method_1852("");
            this.refreshRules();
            this.scrollOffset = Math.max(0, this.rules.size() - this.visibleRows);
         } else {
            this.status = "Tên này đã được bỏ qua, hoặc không thể lưu cấu hình.";
            this.refreshRules();
         }

      }
   }

   public void method_25393() {
      super.method_25393();
      if (this.nameField != null) {
         this.nameField.method_1865();
      }

   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 257 && keyCode != 335) {
         return super.method_25404(keyCode, scanCode, modifiers);
      } else {
         this.addRule();
         return true;
      }
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (button == 0 && mouseX >= (double)this.listLeft && mouseX <= (double)this.listRight && mouseY >= (double)this.listTop && mouseY < (double)this.listBottom) {
         int row = (int)((mouseY - (double)this.listTop) / 23.0);
         int index = this.scrollOffset + row;
         if (index >= 0 && index < this.rules.size()) {
            RaidHelperClient.BossRule rule = (RaidHelperClient.BossRule)this.rules.get(index);
            boolean saved;
            if (mouseX >= (double)(this.listRight - 31)) {
               saved = RaidHelperClient.removeBossRule(rule.name);
               this.status = saved ? "Đã xóa quy tắc: " + rule.name : "Không lưu được thay đổi.";
            } else {
               saved = RaidHelperClient.toggleBossRule(rule.name);
               this.status = saved ? (rule.skipped ? "Đã cho phép boss: " : "Đang bỏ qua boss: ") + rule.name : "Không lưu được thay đổi.";
            }

            this.refreshRules();
            return true;
         }
      }

      return super.method_25402(mouseX, mouseY, button);
   }

   public boolean method_25401(double mouseX, double mouseY, double amount) {
      if (mouseX >= (double)this.listLeft && mouseX <= (double)this.listRight && mouseY >= (double)this.listTop && mouseY < (double)this.listBottom) {
         int maxOffset = Math.max(0, this.rules.size() - this.visibleRows);
         if (amount < 0.0) {
            this.scrollOffset = Math.min(maxOffset, this.scrollOffset + 1);
         } else if (amount > 0.0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 1);
         }

         return true;
      } else {
         return super.method_25401(mouseX, mouseY, amount);
      }
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      this.method_25420(context);
      context.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 12, 16777215);
      context.method_27534(this.field_22793, class_2561.method_43470("Thêm từ khóa để cấm boss. Xem lịch sử boss đã đánh ở nút phía dưới."), this.field_22789 / 2, 29, 13684944);
      context.method_25303(this.field_22793, "DANH SÁCH BOSS", this.listLeft, 75, 16777215);
      if (this.rules.isEmpty()) {
         context.method_25294(this.listLeft, this.listTop, this.listRight, this.listTop + 22, -2008791996);
         context.method_25303(this.field_22793, "Chưa có quy tắc. Nhập tên boss rồi nhấn Thêm.", this.listLeft + 6, this.listTop + 7, 14737632);
      } else {
         for(int visible = 0; visible < this.visibleRows; ++visible) {
            int index = this.scrollOffset + visible;
            if (index >= this.rules.size()) {
               break;
            }

            RaidHelperClient.BossRule rule = (RaidHelperClient.BossRule)this.rules.get(index);
            int y = this.listTop + visible * 23;
            int color = rule.skipped ? -865130199 : -869768646;
            context.method_25294(this.listLeft, y, this.listRight, y + 23 - 2, color);
            String state = rule.skipped ? "[BỎ QUA] " : "[ĐƯỢC CHỌN] ";
            String line = state + rule.name;

            String var10000;
            for(int maxWidth = this.listRight - this.listLeft - 44; this.field_22793.method_1727(line) > maxWidth && line.length() > 5; line = var10000 + "…") {
               var10000 = line.substring(0, line.length() - 2);
            }

            context.method_25303(this.field_22793, line, this.listLeft + 6, y + 7, rule.skipped ? 16777215 : 16777215);
            context.method_25294(this.listRight - 29, y + 2, this.listRight - 3, y + 23 - 4, -12895429);
            context.method_27534(this.field_22793, class_2561.method_43470("X"), this.listRight - 16, y + 5, 16777215);
         }
      }

      context.method_25303(this.field_22793, "Đỏ = bỏ qua | Xanh = không bỏ qua | Cuộn chuột để xem thêm", this.listLeft, this.listBottom + 3, 13684944);
      context.method_25303(this.field_22793, this.status, this.listLeft, this.field_22790 - 47, 16777168);
      super.method_25394(context, mouseX, mouseY, delta);
   }

   public void method_25419() {
      if (this.field_22787 != null) {
         this.field_22787.method_1507(this.parent);
      }

   }

   public boolean method_25421() {
      return false;
   }
}
