package megalodonte.engine.android;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import megalodonte.engine.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Platform-only test runner: exercises real touches, no AndroidX dependency. */
public final class GameSmokeInstrumentation extends Instrumentation {
    private GameActivity activity;
    private View view;
    private long gesture;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        var report=new Bundle();
        try{
            getTargetContext().getSharedPreferences("game-state",0).edit().clear().commit();
            activity=(GameActivity)startActivitySync(new Intent(getTargetContext(),GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            SystemClock.sleep(1200);runOnMainSync(()->{try{var field=GameActivity.class.getDeclaredField("view");field.setAccessible(true);view=(View)field.get(activity);}catch(Exception e){throw new RuntimeException(e);}});
            check(view!=null,"Game view missing");check(read(r->r.scene.id).equals("menu"),"Title did not load");
            Object titleMusic=music();tap("settings");check(music()==titleMusic,"Menu music restarted entering settings");tap("settings-sound");check(music()==titleMusic,"Menu music restarted entering audio submenu");tap("sound-music");SystemClock.sleep(80);check(read(r->r.scene.musicVolume)==0,"Music toggle failed");tap("sound-music");tap("sound-back");tap("settings-display");tap("display-scanlines");check(read(r->r.state.get("scanlines")).equals("true"),"Scanlines failed");tap("display-scanlines");tap("display-back");tap("settings-back");tap("start");tap("difficulty-normal");tap("levels-level1");SystemClock.sleep(300);check(read(r->r.scene.id).equals("normal-1"),"Start button failed");
            capture("race-layout.png");
            double initialX=read(r->alex(r).x),initialY=read(r->alex(r).y);
            float[] right=position("right"),jump=position("jump");gesture=SystemClock.uptimeMillis();
            touch(MotionEvent.ACTION_DOWN,new float[][]{right});SystemClock.sleep(450);
            touch(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[][]{right,jump});SystemClock.sleep(400);
            check(read(r->alex(r).x)>initialX+.4,"Held direction did not move Alex");check(read(r->alex(r).y)>initialY+.4,"Second pointer did not jump while first pointer was held");
            touch(MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[][]{right,jump});touch(MotionEvent.ACTION_UP,new float[][]{right});SystemClock.sleep(100);
            tap("pause");SystemClock.sleep(100);check(read(r->r.paused),"Pause button failed");double elapsed=read(r->r.elapsed);SystemClock.sleep(300);check(Math.abs(read(r->r.elapsed)-elapsed)<.000001,"Paused clock continued");
            tap("resume");SystemClock.sleep(100);check(!read(r->r.paused),"Resume failed");
            tap("pause");SystemClock.sleep(80);tap("retry");SystemClock.sleep(120);check(read(r->alex(r).x)<1,"Restart did not reset Alex");check(read(r->r.state.get("score")).equals("0"),"Restart did not reset score");
            tap("pause");SystemClock.sleep(80);tap("menu");SystemClock.sleep(120);check(read(r->r.scene.id).equals("menu"),"Return to title failed");
            tap("start");tap("difficulty-hard");SystemClock.sleep(120);check(read(r->r.scene.id).equals("menu-difficulty"),"Locked difficulty opened");tap("difficulty-normal");tap("levels-level2");check(read(r->r.scene.id).equals("menu-levels"),"Locked level opened");capture("locked-levels.png");tap("levels-level1");read(r->{alex(r).x=r.scene.worldWidth-5;alex(r).y=7;return true;});SystemClock.sleep(120);check(getTargetContext().getSharedPreferences("game-state",0).getString("state","").contains("progressLevel"),"Progress was not persisted");
            report.putString("result","PASS: title, level select, real multitouch movement+jump, pause/resume, restart and menu");finish(Activity.RESULT_OK,report);
        }catch(Throwable error){report.putString("result","FAIL: "+error);finish(Activity.RESULT_CANCELED,report);}
    }
    private Object music(){var value=new AtomicReference<Object>();runOnMainSync(()->{try{var field=view.getClass().getDeclaredField("music");field.setAccessible(true);value.set(field.get(view));}catch(Exception e){throw new RuntimeException(e);}});return value.get();}
    private void capture(String name)throws Exception{var image=getUiAutomation().takeScreenshot();try(var stream=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}image.recycle();}
    private static Game.Entity alex(megalodonte.engine.Runtime r){return r.scene.objects.stream().filter(e->e.id.equals("alex")).findFirst().orElseThrow();}
    private <T>T read(Function<megalodonte.engine.Runtime,T> getter){var result=new AtomicReference<T>();runOnMainSync(()->{try{var field=view.getClass().getDeclaredField("runtime");field.setAccessible(true);result.set(getter.apply((megalodonte.engine.Runtime)field.get(view)));}catch(Exception e){throw new RuntimeException(e);}});return result.get();}
    private float[] position(String id){return read(r->{var hud=r.scene.hud.stream().filter(h->h.id.equals(id)).findFirst().orElseThrow();check(hud.visible,"Hidden button: "+id);var box=HudLayout.resolve(hud,view.getWidth(),view.getHeight());int[] origin=new int[2];view.getLocationOnScreen(origin);return new float[]{origin[0]+(float)(box.x()+box.width()/2),origin[1]+(float)(box.y()+box.height()/2)};});}
    private void tap(String id){float[] point=position(id);gesture=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,new float[][]{point});SystemClock.sleep(60);touch(MotionEvent.ACTION_UP,new float[][]{point});}
    private void touch(int action,float[][] points){
        var properties=new MotionEvent.PointerProperties[points.length];var coordinates=new MotionEvent.PointerCoords[points.length];
        for(int i=0;i<points.length;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coordinates[i]=new MotionEvent.PointerCoords();coordinates[i].x=points[i][0];coordinates[i].y=points[i][1];coordinates[i].pressure=1;coordinates[i].size=1;}
        var event=MotionEvent.obtain(gesture,SystemClock.uptimeMillis(),action,points.length,properties,coordinates,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);try{sendPointerSync(event);}finally{event.recycle();}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
