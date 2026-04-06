package foure.dev.event.impl.input;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.screen.slot.SlotActionType;

public class ClickSlotEvent extends Event {
   private int windowId;
   private int slotId;
   private int button;
   private SlotActionType actionType;

   @Generated
   public int getWindowId() {
      return this.windowId;
   }

   @Generated
   public int getSlotId() {
      return this.slotId;
   }

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public SlotActionType getActionType() {
      return this.actionType;
   }

   @Generated
   public void setWindowId(int windowId) {
      this.windowId = windowId;
   }

   @Generated
   public void setSlotId(int slotId) {
      this.slotId = slotId;
   }

   @Generated
   public void setButton(int button) {
      this.button = button;
   }

   @Generated
   public void setActionType(SlotActionType actionType) {
      this.actionType = actionType;
   }

   @Generated
   public ClickSlotEvent(int windowId, int slotId, int button, SlotActionType actionType) {
      this.windowId = windowId;
      this.slotId = slotId;
      this.button = button;
      this.actionType = actionType;
   }
}
