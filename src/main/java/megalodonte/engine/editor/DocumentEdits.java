package megalodonte.engine.editor;
import megalodonte.engine.*;
/** Validated transactions keep references coherent before changing the live document. */
final class DocumentEdits {
    private DocumentEdits(){}
    static Game entity(Game source,int sceneIndex,int entityIndex,Game.Entity value){
        var copy=Codec.copy(source);var scene=copy.scenes.get(sceneIndex);String old=scene.objects.get(entityIndex).id;var replacement=value.copy();scene.objects.set(entityIndex,replacement);
        if(!old.equals(replacement.id)){for(var child:scene.objects)if(child.parentId.equals(old))child.parentId=replacement.id;if(scene.camera.targetId.equals(old))scene.camera.targetId=replacement.id;}
        Codec.validate(copy);return copy;
    }
    static Game scene(Game source,int index,Game.Scene value){
        var copy=Codec.copy(source);String old=copy.scenes.get(index).id;copy.scenes.set(index,Codec.GSON.fromJson(Codec.GSON.toJson(value),Game.Scene.class));if(copy.entrySceneId.equals(old))copy.entrySceneId=value.id;if(copy.lastEditedSceneId.equals(old))copy.lastEditedSceneId=value.id;Codec.validate(copy);return copy;
    }
}
