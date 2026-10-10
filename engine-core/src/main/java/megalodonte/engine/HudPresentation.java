package megalodonte.engine;
/** Same declarative property evaluation in authoring, Desktop and Android. */
public final class HudPresentation {
    private HudPresentation(){}
    public static String asset(Game.Hud hud){var c=hud.condition;if(c.property.isBlank())return hud.assetPath;String path=hud.booleanProperty(c.property,false)?c.trueAssetPath:c.falseAssetPath;return path.isBlank()?hud.assetPath:path;}
    public static boolean enabled(Game.Hud hud){return !hud.condition.disableWhenFalse||hud.condition.property.isBlank()||hud.booleanProperty(hud.condition.property,false);}
    public static void validate(Game.Hud hud){
        if(hud.properties==null||hud.condition==null)throw new IllegalArgumentException("Propriedades/condição da HUD inválidas");
        for(var entry:hud.properties.entrySet()){var value=entry.getValue();if(entry.getKey()==null||entry.getKey().isBlank()||value==null||(value.isNumber()&&!Double.isFinite(value.getAsDouble())))throw new IllegalArgumentException("Propriedade HUD inválida");}
        var c=hud.condition;if(c.property==null)throw new IllegalArgumentException("Propriedade condicional inválida");Codec.validateAsset(c.trueAssetPath);Codec.validateAsset(c.falseAssetPath);
        if(!c.property.isBlank()){var value=hud.properties.get(c.property);if(value==null||!value.isBoolean())throw new IllegalArgumentException("Condição deve apontar para uma propriedade booleana existente: "+c.property);}
    }
}
