package litematica.gui.util;

import java.nio.file.Path;
import java.util.HashMap;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.NBTTagCompound;

import malilib.util.data.Identifier;
import malilib.util.nbt.NbtUtils;
import litematica.Reference;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.SchematicType;

public class SchematicInfoCache
{
    protected final HashMap<Path, SchematicInfo> cachedData = new HashMap<>();
    protected final Minecraft mc = Minecraft.getMinecraft();

    @Nullable
    public SchematicInfo getSchematicInfo(Path file)
    {
        return this.cachedData.get(file);
    }

    public void clearCache()
    {
        for (SchematicInfo info : this.cachedData.values())
        {
            if (info != null && info.texture != null)
            {
                this.mc.getTextureManager().deleteTexture(info.iconName);
            }
        }

        this.cachedData.clear();
    }

    @Nullable
    public SchematicInfo getOrCacheSchematicInfo(Path file)
    {
        SchematicInfo info = this.cachedData.get(file);

        if (info != null)
        {
            return info;
        }

        // TODO Use a partial NBT read method to only read the metadata tag
        // TODO (that's only beneficial if it's stored before the bulk schematic data in the stream)
        NBTTagCompound tag = NbtUtils.readNbtFromFile(file);
        SchematicInfo data = null;

        if (tag != null)
        {
            ISchematic schematic = SchematicType.tryCreateSchematicFrom(file, tag);

            if (schematic != null)
            {
                SchematicMetadata metadata = schematic.getMetadata();
                Identifier iconName = new Identifier(Reference.MOD_ID, file.toAbsolutePath().toString());
                DynamicTexture texture = this.createPreviewImage(iconName, metadata);
                data = new SchematicInfo(schematic, iconName, texture);
            }
        }

        this.cachedData.put(file, data);

        return data;
    }

    @Nullable
    protected DynamicTexture createPreviewImage(Identifier iconName, SchematicMetadata meta)
    {
        int[] previewImageData = meta.getPreviewImagePixelData();

        if (previewImageData != null && previewImageData.length > 0)
        {
            try
            {
                int size = (int) Math.sqrt(previewImageData.length);

                if (size * size == previewImageData.length)
                {
                    //BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                    //buf.setRGB(0, 0, size, size, previewImageData, 0, size);

                    DynamicTexture tex = new DynamicTexture(size, size);
                    this.mc.getTextureManager().loadTexture(iconName, tex);

                    System.arraycopy(previewImageData, 0, tex.getTextureData(), 0, previewImageData.length);
                    tex.updateDynamicTexture();

                    return tex;
                }
            }
            catch (Exception ignore) {}
        }

        return null;
    }

    public static class SchematicInfo
    {
        public final ISchematic schematic;
        public final Identifier iconName;
        @Nullable public final DynamicTexture texture;

        protected SchematicInfo(ISchematic schematic,
                                Identifier iconName,
                                @Nullable DynamicTexture texture)
        {
            this.schematic = schematic;
            this.iconName = iconName;
            this.texture = texture;
        }
    }
}
