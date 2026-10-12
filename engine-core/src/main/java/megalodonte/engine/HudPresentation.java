package megalodonte.engine;
/** Same declarative property evaluation in authoring, Desktop and Android. */
public final class HudPresentation {
    private HudPresentation(){}
    public static String asset(Game.Hud hud){var c=hud.condition;if(c.property.isBlank())return hud.assetPath;String path=hud.booleanProperty(c.property,false)?c.trueAssetPath:c.falseAssetPath;return path.isBlank()?hud.assetPath:path;}
    public static boolean enabled(Game.Hud hud){return !hud.condition.disableWhenFalse||hud.condition.property.isBlank()||hud.booleanProperty(hud.condition.property,false);}
    public static void validate(Game.Hud hud){
        if(hud.buttonBackground==null||!hud.buttonBackground.matches("#[0-9a-fA-F]{6}")||hud.fontFamily==null||hud.textAlign==null||hud.textVerticalAlign==null||!java.util.Set.of("left","center","right").contains(hud.textAlign)||!java.util.Set.of("top","center","bottom").contains(hud.textVerticalAlign)||!Double.isFinite(hud.buttonOpacity)||hud.buttonOpacity<0||hud.buttonOpacity>1||!Double.isFinite(hud.cornerRadius)||hud.cornerRadius<0)throw new IllegalArgumentException("Estilo da HUD inválido");
        if(hud.properties==null||hud.condition==null)throw new IllegalArgumentException("Propriedades/condição da HUD inválidas");
        for(var entry:hud.properties.entrySet()){var value=entry.getValue();if(entry.getKey()==null||entry.getKey().isBlank()||value==null||(value.isNumber()&&!Double.isFinite(value.getAsDouble())))throw new IllegalArgumentException("Propriedade HUD inválida");}
        var c=hud.condition;if(c.property==null)throw new IllegalArgumentException("Propriedade condicional inválida");Codec.validateAsset(c.trueAssetPath);Codec.validateAsset(c.falseAssetPath);
        if(!c.property.isBlank()){var value=hud.properties.get(c.property);if(value==null||!value.isBoolean())throw new IllegalArgumentException("Condição deve apontar para uma propriedade booleana existente: "+c.property);}
    }
}
