package com.raidhub.client;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public final class RaidHelperSettingsScreen extends class_437 {
   private final class_437 parent;
   private class_342 minDelayField;
   private class_342 maxDelayField;
   private class_4185 selectionModeButton;
   private String status = "Chọn tuần tự theo hàng hoặc ngẫu nhiên; chỉnh thời gian chờ sau boss.";

   public RaidHelperSettingsScreen(class_437 parent) {
      super(class_2561.method_43470("RaidHelper - Cài đặt"));
      this.parent = parent;
   }

   protected void method_25426() {
      int center = this.field_22789 / 2;
      int fieldX = center + 28;
      this.minDelayField = new class_342(this.field_22793, fieldX, 58, 82, 20, class_2561.method_43470("Thời gian tối thiểu"));
      this.minDelayField.method_1880(3);
      this.minDelayField.method_1852(Integer.toString(RaidHelperClient.getCompletionDelayMinSeconds()));
      this.minDelayField.method_1887("giây");
      this.method_37063(this.minDelayField);
      this.maxDelayField = new class_342(this.field_22793, fieldX, 91, 82, 20, class_2561.method_43470("Thời gian tối đa"));
      this.maxDelayField.method_1880(3);
      this.maxDelayField.method_1852(Integer.toString(RaidHelperClient.getCompletionDelayMaxSeconds()));
      this.maxDelayField.method_1887("giây");
      this.method_37063(this.maxDelayField);
      this.method_48265(this.minDelayField);
      this.selectionModeButton = class_4185.method_46430(this.selectionModeButtonText(), (button) -> {
         this.status = RaidHelperClient.cycleBossSelectionMode();
         button.method_25355(this.selectionModeButtonText());
      }).method_46434(center - 105, 123, 210, 20).method_46431();
      this.method_37063(this.selectionModeButton);
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Lưu thời gian"), (button) -> {
         this.saveSettings();
      }).method_46434(center - 152, 159, 104, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Mặc định 4–5s"), (button) -> {
         this.resetSettings();
      }).method_46434(center - 42, 159, 116, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Quay lại"), (button) -> {
         this.method_25419();
      }).method_46434(center + 82, 159, 70, 20).method_46431());
   }

   private class_2561 selectionModeButtonText() {
      return class_2561.method_43470("Cách chọn boss: " + RaidHelperClient.getBossSelectionModeLabel());
   }

   private void saveSettings() {
      String result = RaidHelperClient.saveCompletionDelaySettings(this.minDelayField.method_1882(), this.maxDelayField.method_1882());
      this.status = result == null ? "Đã lưu. Chờ sau boss: " + RaidHelperClient.getCompletionDelayMinSeconds() + "–" + RaidHelperClient.getCompletionDelayMaxSeconds() + " giây." : result;
   }

   private void resetSettings() {
      String result = RaidHelperClient.resetCompletionDelaySettings();
      this.minDelayField.method_1852(Integer.toString(RaidHelperClient.getCompletionDelayMinSeconds()));
      this.maxDelayField.method_1852(Integer.toString(RaidHelperClient.getCompletionDelayMaxSeconds()));
      this.status = result == null ? "Đã khôi phục thời gian mặc định 4–5 giây." : result;
   }

   public void method_25393() {
      super.method_25393();
      if (this.minDelayField != null) {
         this.minDelayField.method_1865();
      }

      if (this.maxDelayField != null) {
         this.maxDelayField.method_1865();
      }

   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 257 && keyCode != 335) {
         return super.method_25404(keyCode, scanCode, modifiers);
      } else {
         this.saveSettings();
         return true;
      }
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      this.method_25420(context);
      context.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 18, 16777215);
      context.method_27534(this.field_22793, class_2561.method_43470("Thời gian chờ và cách chọn boss"), this.field_22789 / 2, 36, 13684944);
      context.method_25303(this.field_22793, "Tối thiểu (giây):", this.field_22789 / 2 - 116, 64, 16777215);
      context.method_25303(this.field_22793, "Tối đa (giây):", this.field_22789 / 2 - 116, 97, 16777215);
      context.method_27534(this.field_22793, class_2561.method_43470("Tuần tự: hết hàng trái → phải, rồi xuống hàng | Ngẫu nhiên"), this.field_22789 / 2, 112, 12303291);
      context.method_27534(this.field_22793, class_2561.method_43470("Thời gian: số nguyên từ 1 đến 300 giây."), this.field_22789 / 2, 145, 12303291);
      context.method_27534(this.field_22793, class_2561.method_43470(this.status), this.field_22789 / 2, 192, 16777168);
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
