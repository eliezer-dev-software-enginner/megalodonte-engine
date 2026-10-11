package megalodonte.engine;
/** Shared orthographic projection; zoom > 1 moves closer. */
public record CameraView(double scale,double originX,double originY,double left,double bottom,double width,double height) {
    public static CameraView of(Game.Camera camera,double pixelWidth,double pixelHeight) {
        double width=camera.width/camera.zoom,height=camera.height/camera.zoom;
        double top=Math.min(pixelHeight,camera.viewportTop*Math.min(pixelWidth/1280,pixelHeight/720));
        double availableHeight=pixelHeight-top;
        double scale=Math.max(0,Math.min(pixelWidth/width,availableHeight/height));
        double left=camera.x-width/2,bottom=camera.y-height/2;
        return new CameraView(scale,(pixelWidth-width*scale)/2-left*scale,
            top+(availableHeight-height*scale)/2+(bottom+height)*scale,left,bottom,width,height);
    }
    public double screenX(double x){return originX+x*scale;}
    public double screenY(double y){return originY-y*scale;}
    public double worldX(double x){return (x-originX)/scale;}
    public double worldY(double y){return (originY-y)/scale;}
    public static void update(Game.Scene scene,double dt){update(scene,dt,true);}
    public static void update(Game.Scene scene,double dt,boolean targetGrounded){
        var camera=scene.camera;
        if(camera.mode.equals("follow")&&!camera.targetId.isEmpty()){
            var target=Hierarchy.find(scene,camera.targetId);var pose=Hierarchy.pose(scene,target);
            double x=pose.x()+target.width/2+camera.followOffsetX,y=pose.y()+target.height/2+camera.followOffsetY;
            double dx=x-camera.x,dy=y-camera.y;
            dx=Math.copySign(Math.max(0,Math.abs(dx)-camera.deadZoneWidth/2),dx);
            dy=Math.copySign(Math.max(0,Math.abs(dy)-camera.deadZoneHeight/2),dy);
            if(camera.holdGroundDuringJump&&!targetGrounded&&dy>0){
                // Keep the floor framed, except when the actor would leave the viewport.
                double head=Runtime.collisionBounds(scene,target).y()+Runtime.collisionBounds(scene,target).height();
                dy=Math.max(0,head-camera.y-camera.height/camera.zoom/2+.25);
            }
            double blend=camera.followSpeed==0?1:1-Math.exp(-camera.followSpeed*Math.max(0,dt));
            camera.x+=dx*blend;camera.y+=dy*blend;
        }
        if(camera.clampToWorld){
            double halfX=camera.width/camera.zoom/2,halfY=camera.height/camera.zoom/2;
            camera.x=scene.worldWidth<=halfX*2?scene.worldWidth/2:Math.max(halfX,Math.min(scene.worldWidth-halfX,camera.x));
            camera.y=scene.worldHeight<=halfY*2?scene.worldHeight/2:Math.max(halfY,Math.min(scene.worldHeight-halfY,camera.y));
        }
    }
}
