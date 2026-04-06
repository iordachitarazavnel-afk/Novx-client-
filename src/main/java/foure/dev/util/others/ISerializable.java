package foure.dev.util.others;

import net.minecraft.nbt.NbtCompound;

public interface ISerializable<T> {
   NbtCompound toTag();

   T fromTag(NbtCompound var1);
}
