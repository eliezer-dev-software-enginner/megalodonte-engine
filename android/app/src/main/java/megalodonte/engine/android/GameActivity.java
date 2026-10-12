package megalodonte.engine.android;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.media.MediaPlayer;
import megalodonte.engine.*;
import java.util.*;

public final class GameActivity extends Activity {
    private String exportedSave;
    private java.util.function.Consumer<String> exportDone;
    private java.util.function.BiConsumer<String,String> importDone;
    private void exportSaveFile(String json,java.util.function.Consumer<String> done){
        exportedSave=json;exportDone=done;
        try{startActivityForResult(new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).addCategory(android.content.Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(android.content.Intent.EXTRA_TITLE,"alex-vs-bus-save.json"),701);}catch(Exception e){exportedSave=null;exportDone=null;done.accept("Falha ao exportar: "+e.getMessage());}
    }
    private void importSaveFile(java.util.function.BiConsumer<String,String> done){
        importDone=done;
        try{startActivityForResult(new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).addCategory(android.content.Intent.CATEGORY_OPENABLE).setType("*/*"),702);}catch(Exception e){importDone=null;done.accept(null,"Falha ao importar: "+e.getMessage());}
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);
        if(request==701&&exportDone!=null){var done=exportDone;exportDone=null;String json=exportedSave;exportedSave=null;
            if(result!=RESULT_OK||data==null||data.getData()==null){done.accept("Exportação cancelada.");return;}
            try(var out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new java.io.IOException("Destino indisponível");out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));done.accept("Dados exportados.");}catch(Exception e){done.accept("Falha ao exportar: "+e.getMessage());}
        }else if(request==702&&importDone!=null){var done=importDone;importDone=null;
            if(result!=RESULT_OK||data==null||data.getData()==null){done.accept(null,"Importação cancelada.");return;}
            try(var in=getContentResolver().openInputStream(data.getData());var out=new java.io.ByteArrayOutputStream()){
                if(in==null)throw new java.io.IOException("Arquivo indisponível");byte[] bytes=new byte[4096];int n;
                while((n=in.read(bytes))!=-1){if(out.size()+n>SaveData.MAX_BYTES)throw new java.io.IOException("Arquivo muito grande");out.write(bytes,0,n);}
                done.accept(out.toString(java.nio.charset.StandardCharsets.UTF_8),null);
            }catch(Exception e){done.accept(null,"Falha ao importar: "+e.getMessage());}
        }
    }
    private GameView view;
    @Override public void onCreate(Bundle state){super.onCreate(state);try{
        String json;try(var stream=getAssets().open("game.json")){json=new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        view=new GameView(Codec.read(json));setSafeContent(view);controllerManager=(android.hardware.input.InputManager)getSystemService(INPUT_SERVICE);controllerManager.registerInputDeviceListener(controllerListener,null);
    }catch(Exception e){android.widget.TextView error=new android.widget.TextView(this);error.setText("Falha ao carregar jogo: "+e.getMessage());setSafeContent(error);}}
    private void setSafeContent(View content){
        var host=new android.widget.FrameLayout(this);host.setBackgroundColor(Color.BLACK);
        host.addView(content,new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        host.setOnApplyWindowInsetsListener((root,insets)->{var safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});setContentView(host);host.requestApplyInsets();host.post(()->{var controller=host.getWindowInsetsController();if(controller!=null){controller.hide(WindowInsets.Type.statusBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}});
    }
    private final GamepadInput gamepad=new GamepadInput();
    private double stickX,stickY,hatX,hatY;
    private android.hardware.input.InputManager controllerManager;
    private final android.hardware.input.InputManager.InputDeviceListener controllerListener=new android.hardware.input.InputManager.InputDeviceListener(){public void onInputDeviceAdded(int id){}public void onInputDeviceChanged(int id){}public void onInputDeviceRemoved(int id){clearGamepad();}};
    private int gamepadDirection(String direction){return gamepad.held(direction)?1:0;}
    private void clearGamepad(){gamepad.clear();stickX=stickY=hatX=hatY=0;}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){
        if(view!=null&&(e.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK&&e.getAction()==MotionEvent.ACTION_MOVE){
            stickX=controllerAxis(e,MotionEvent.AXIS_X);stickY=controllerAxis(e,MotionEvent.AXIS_Y);hatX=controllerAxis(e,MotionEvent.AXIS_HAT_X);hatY=controllerAxis(e,MotionEvent.AXIS_HAT_Y);gamepad.axes(stickX,stickY,hatX,hatY);return true;
        }return super.dispatchGenericMotionEvent(e);
    }
    private double controllerAxis(MotionEvent e,int axis){var range=e.getDevice()==null?null:e.getDevice().getMotionRange(axis,e.getSource());double value=e.getAxisValue(axis);return Math.abs(value)<=Math.max(.2,range==null?0:range.getFlat())?0:value;}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(view!=null&&((e.getDevice()!=null&&e.getDevice().supportsSource(InputDevice.SOURCE_GAMEPAD))||(e.getSource()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD||(e.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK)){
            String name=switch(e.getKeyCode()){case KeyEvent.KEYCODE_BUTTON_A->"a";case KeyEvent.KEYCODE_BUTTON_B->"b";case KeyEvent.KEYCODE_BUTTON_X->"x";case KeyEvent.KEYCODE_BUTTON_Y->"y";case KeyEvent.KEYCODE_BUTTON_L1->"l1";case KeyEvent.KEYCODE_BUTTON_R1->"r1";case KeyEvent.KEYCODE_BUTTON_START->"start";case KeyEvent.KEYCODE_DPAD_LEFT->"left";case KeyEvent.KEYCODE_DPAD_RIGHT->"right";case KeyEvent.KEYCODE_DPAD_UP->"up";case KeyEvent.KEYCODE_DPAD_DOWN->"down";default->"";};
            if(!name.isEmpty()){gamepad.button(name,e.getAction()==KeyEvent.ACTION_DOWN);return true;}
        }return super.dispatchKeyEvent(e);
    }
    @Override protected void onPause(){super.onPause();clearGamepad();if(view!=null){view.active=false;view.clearInput();view.effects.autoPause();view.pendingSounds.clear();if(view.music!=null&&view.musicReady)view.music.pause();}}
    @Override protected void onResume(){super.onResume();if(view!=null){view.active=true;view.last=0;if(!view.audioPaused)view.effects.autoResume();if(view.music!=null&&view.musicReady&&!view.runtime.paused)view.music.start();view.postInvalidateOnAnimation();}}
    @Override protected void onDestroy(){if(controllerManager!=null)controllerManager.unregisterInputDeviceListener(controllerListener);if(view!=null)view.close();super.onDestroy();}
    private final class GameView extends View {
        final megalodonte.engine.Runtime runtime;
        final Paint paint=new Paint();
        final Map<String,Bitmap> images=new java.util.LinkedHashMap<>(32,.75f,true);
        long imageBytes;
        final Map<Integer,String> touchActions=new HashMap<>();
        final Set<String> held=new HashSet<>();
        final android.media.SoundPool effects=new android.media.SoundPool.Builder().setMaxStreams(12)
            .setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_GAME).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        final Map<String,Integer> samples=new HashMap<>();
        final Set<Integer> readySamples=new HashSet<>(),pendingSounds=new HashSet<>();
        final java.util.ArrayDeque<Integer> streams=new java.util.ArrayDeque<>();
        Game.Scene soundScene;
        final Set<String> loadingImages=new HashSet<>(),failedImages=new HashSet<>();
        final java.util.concurrent.ThreadPoolExecutor imageLoader=new java.util.concurrent.ThreadPoolExecutor(1,1,0,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<>(64));
        boolean disposed;
        MediaPlayer music;boolean musicReady;String musicScene="";
        boolean muted;boolean audioPaused;boolean active=true;long last;double dx,dy;
        GameView(Game game){super(GameActivity.this);paint.setFilterBitmap(false);
            effects.setOnLoadCompleteListener((pool,id,status)->{
                if(disposed||!samples.containsValue(id))return;if(status==0){readySamples.add(id);if(pendingSounds.remove(id))playSample(id);}
                else pendingSounds.remove(id);
            });
            runtime=new megalodonte.engine.Runtime(game,megalodonte.engine.Runtime.reflection(getClass().getClassLoader()),new megalodonte.engine.Runtime.Events(){
                public java.util.Map<String,String> loadState(){try{var type=new com.google.gson.reflect.TypeToken<java.util.Map<String,String>>(){}.getType();java.util.Map<String,String> state=Codec.GSON.fromJson(getSharedPreferences("game-state",0).getString("state","{}"),type);return state==null?java.util.Map.of():state;}catch(Exception e){return java.util.Map.of();}}
                public void saveState(java.util.Map<String,String> state){getSharedPreferences("game-state",0).edit().putString("state",Codec.GSON.toJson(state)).apply();}
                public void exportSave(String json,java.util.function.Consumer<String> done){exportSaveFile(json,done);}
                public void importSave(java.util.function.BiConsumer<String,String> done){importSaveFile(done);}
                public void log(String message){android.util.Log.i("MegalodonteScript",message);}
                public void playSound(String asset){sound(asset);}
                public void quit(){GameActivity.this.finish();}
                public void muteAudio(boolean value){muted=value;if(music!=null){float v=value?0:(float)runtime.scene.musicVolume;music.setVolume(v,v);}if(value){for(int stream:streams)effects.stop(stream);streams.clear();pendingSounds.clear();}}
                public void pauseAudio(boolean value){audioPaused=value;if(music!=null&&musicReady){if(value)music.pause();else if(active)music.start();}if(value){effects.autoPause();pendingSounds.clear();}else if(active)effects.autoResume();}
            });
        }
        void loadImage(String path){
            if(disposed||path.isBlank()||images.containsKey(path)||failedImages.contains(path)||!loadingImages.add(path))return;
            try{imageLoader.execute(()->{
                Bitmap decoded=null;try(var input=getAssets().open(path)){decoded=BitmapFactory.decodeStream(input);}catch(Exception error){android.util.Log.w("Megalodonte",path,error);}
                final Bitmap bitmap=decoded;
                post(()->{loadingImages.remove(path);if(disposed){if(bitmap!=null)bitmap.recycle();return;}
                    if(bitmap==null){failedImages.add(path);return;}
                    images.put(path,bitmap);imageBytes+=bitmap.getAllocationByteCount();
                    while(imageBytes>64L*1024*1024&&images.size()>1){var iterator=images.entrySet().iterator();var entry=iterator.next();imageBytes-=entry.getValue().getAllocationByteCount();iterator.remove();}
                    // Do not recycle an evicted bitmap that the hardware renderer may still reference.
                    if(active)postInvalidateOnAnimation();
                });
            });}catch(java.util.concurrent.RejectedExecutionException busy){loadingImages.remove(path);}
        }
        Bitmap image(String path){loadImage(path);return images.get(path);}
        void prefetchFrames(Game.Entity entity){
            var animation=entity.animations.get(entity.animation);if(animation==null||animation.frames.isEmpty())return;
            int index=(int)(entity.animationTime*animation.fps);int count=animation.frames.size();
            for(int offset=0;offset<3;offset++){int frame=animation.loop?(index+offset)%count:Math.min(count-1,index+offset);loadImage(animation.frames.get(frame));}
        }
        int loadSound(String asset){
            var sample=samples.get(asset);if(sample!=null)return sample;
            try(var fd=getAssets().openFd(asset)){int id=effects.load(fd,1);if(id!=0)samples.put(asset,id);return id;}
            catch(Exception error){android.util.Log.e("MegalodonteAudio",asset,error);return 0;}
        }
        void preloadSounds(){
            if(soundScene==runtime.scene)return;soundScene=runtime.scene;
            var wanted=new HashSet<String>();for(var entity:soundScene.objects)for(var property:entity.properties.entrySet())
                if(property.getKey().endsWith("Sound")&&!property.getValue().isBlank())wanted.add(property.getValue());
            // Keep only the current scene's short effects, never the entire campaign's audio.
            for(var iterator=samples.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();if(!wanted.contains(entry.getKey())){
                effects.unload(entry.getValue());readySamples.remove(entry.getValue());pendingSounds.remove(entry.getValue());iterator.remove();}}
            for(String asset:wanted)loadSound(asset);
        }
        void playSample(int id){if(muted||audioPaused||!active||disposed)return;
            int stream=effects.play(id,1,1,1,0,1);if(stream!=0){streams.addLast(stream);while(streams.size()>12)effects.stop(streams.removeFirst());}
        }
        MediaPlayer audio(String asset)throws Exception{MediaPlayer player=new MediaPlayer();try(var fd=getAssets().openFd(asset)){player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}catch(Exception e){player.release();throw e;}return player;}
        void sound(String asset){if(muted||audioPaused||!active||disposed)return;
            int sample=loadSound(asset);if(sample==0)return;if(readySamples.contains(sample))playSample(sample);else pendingSounds.add(sample);
        }
        void updateMusic(){
            if(music!=null){float volume=muted?0:(float)runtime.scene.musicVolume;music.setVolume(volume,volume);}
            String key=MusicTrack.key(runtime.scene);if(key.equals(musicScene))return;
            if(music!=null){music.release();music=null;}musicReady=false;musicScene=key;
            if(runtime.scene.musicAssetPath.isBlank())return;
            try{
                music=audio(runtime.scene.musicAssetPath);var player=music;
                player.setLooping(runtime.scene.musicLoop);float volume=muted?0:(float)runtime.scene.musicVolume;player.setVolume(volume,volume);
                player.setOnPreparedListener(ready->{if(disposed||music!=ready)return;musicReady=true;if(active&&!audioPaused&&!runtime.paused)ready.start();});
                player.setOnErrorListener((failed,what,extra)->{if(music==failed){music=null;musicReady=false;}failed.release();android.util.Log.e("MegalodonteAudio","Música indisponível: "+what+"/"+extra);return true;});
                player.prepareAsync();
            }catch(Exception error){if(music!=null){music.release();music=null;}android.util.Log.e("MegalodonteAudio","Falha na música",error);}
        }
        @Override protected void onDraw(Canvas canvas){
            if(!active)return;
            long now=System.nanoTime();try{preloadSounds();gamepad.apply(runtime);runtime.update(last==0?0:(now-last)/1e9,dx+gamepad.horizontal(),dy);updateMusic();}catch(Exception e){active=false;clearInput();android.widget.Toast.makeText(GameActivity.this,"Erro no script: "+e.getMessage(),android.widget.Toast.LENGTH_LONG).show();return;}last=now;
            var scene=runtime.scene;var view=CameraView.of(scene.camera,getWidth(),getHeight());float scale=(float)view.scale(),ox=(float)view.originX(),oy=(float)view.originY();
            canvas.drawColor(Color.BLACK);canvas.save();canvas.clipRect((float)view.screenX(view.left()),(float)view.screenY(view.bottom()+view.height()),(float)view.screenX(view.left()+view.width()),(float)view.screenY(view.bottom()));paint.setColor(Color.parseColor(scene.background));paint.setAlpha(255);canvas.drawRect(ox,oy-(float)scene.worldHeight*scale,ox+(float)scene.worldWidth*scale,oy,paint);
            BackgroundDraw.render(scene,view,(asset,x,y,w,h)->{Bitmap bitmap=image(asset);if(bitmap!=null)canvas.drawBitmap(bitmap,null,new RectF((float)x,(float)y,(float)(x+w),(float)(y+h)),paint);});
            TileDraw.render(scene,view,(tile,x,y,w,h,alpha,a,b,c,d)->{Bitmap image=image(tile.assetPath);if(image==null)return;canvas.save();float left=(float)Math.floor(x),top=(float)Math.floor(y),right=(float)Math.ceil(x+w),bottom=(float)Math.ceil(y+h);canvas.translate((left+right)/2,(top+bottom)/2);canvas.scale(right-left,bottom-top);Matrix matrix=new Matrix();matrix.setValues(new float[]{(float)a,(float)c,0,(float)b,(float)d,0,0,0,1});canvas.concat(matrix);paint.setAlpha((int)(alpha*255));canvas.drawBitmap(image,new Rect(tile.sourceX,tile.sourceY,tile.sourceX+tile.width,tile.sourceY+tile.height),new RectF(-.5f,-.5f,.5f,.5f),paint);canvas.restore();});paint.setAlpha(255);
            for(var e:DrawOrder.visibleEntities(scene,view)){if(!e.visible)continue;var pose=Hierarchy.pose(scene,e);RectF rect=new RectF(ox+(float)pose.x()*scale,oy-(float)(pose.y()+e.height)*scale,ox+(float)(pose.x()+e.width)*scale,oy-(float)pose.y()*scale);canvas.save();paint.setAlpha((int)(e.opacity*255));canvas.rotate((float)(pose.rotation()%360),rect.centerX(),rect.centerY());if(e.flipX)canvas.scale(-1,1,rect.centerX(),rect.centerY());prefetchFrames(e);Bitmap image=image(Sprites.asset(e));if(image!=null)canvas.drawBitmap(image,null,rect,paint);else if(Sprites.asset(e).isBlank()){paint.setColor(Color.parseColor(e.color));canvas.drawRect(rect,paint);}canvas.restore();paint.setAlpha(255);}
            canvas.restore();
            for(var hud:scene.hud){if(!hud.visible)continue;String hudAsset=HudPresentation.asset(hud);loadImage(hudAsset);var b=HudLayout.resolve(hud,getWidth(),getHeight());canvas.save();canvas.clipRect((float)b.x(),(float)b.y(),(float)(b.x()+b.width()),(float)(b.y()+b.height()));
                RectF rect=new RectF((float)b.x(),(float)b.y(),(float)(b.x()+b.width()),(float)(b.y()+b.height()));
                if(hud.type.equals("button")&&hudAsset.isBlank()){paint.setColor(Color.parseColor(hud.buttonBackground));paint.setAlpha((int)(hud.buttonOpacity*255));canvas.drawRoundRect(rect,(float)(hud.cornerRadius*b.scale()),(float)(hud.cornerRadius*b.scale()),paint);paint.setAlpha(255);}
                if(hud.type.equals("button")&&!hudAsset.isBlank()){Bitmap button=images.get(hudAsset);if(button!=null)canvas.drawBitmap(button,null,rect,paint);}
                paint.setColor(Color.parseColor(hud.color));
                if(hud.type.equals("text")||hud.type.equals("button")){paint.setAntiAlias(true);paint.setTextSize((float)(hud.fontSize*b.scale()));paint.setTypeface(hud.fontFamily.isBlank()?android.graphics.Typeface.DEFAULT:android.graphics.Typeface.create(hud.fontFamily,android.graphics.Typeface.NORMAL));paint.setTextAlign(Paint.Align.valueOf(hud.textAlign.toUpperCase(java.util.Locale.ROOT)));var lines=hud.text.split("\\R",-1);float lineHeight=(float)(hud.fontSize*b.scale()*1.2),remaining=(float)b.height()-lineHeight*lines.length;float lineY=(float)b.y()-paint.getFontMetrics().top+switch(hud.textVerticalAlign){case "center"->remaining/2;case "bottom"->remaining;default->0;};float textX=(float)b.x()+switch(hud.textAlign){case "center"->(float)b.width()/2;case "right"->(float)b.width();default->0;};for(String line:lines){canvas.drawText(line,textX,lineY,paint);lineY+=lineHeight;}paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(android.graphics.Typeface.DEFAULT);paint.setAntiAlias(false);}
                else{Bitmap image=images.get(hudAsset);if(image!=null)canvas.drawBitmap(image,null,rect,paint);else canvas.drawRect(rect,paint);}
                if(hud.focused){paint.setColor(Color.GREEN);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);canvas.drawRect(rect.left+2,rect.top+2,rect.right-2,rect.bottom-2,paint);paint.setStyle(Paint.Style.FILL);}
                canvas.restore();
            }
            if(runtime.fadeAlpha()>0){paint.setColor(Color.argb((int)(runtime.fadeAlpha()*255),0,0,0));canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
            postInvalidateOnAnimation();
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){clearInput();return true;}
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN||event.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){int i=event.getActionIndex();String action="";
                for(int j=runtime.scene.hud.size()-1;j>=0;j--){var h=runtime.scene.hud.get(j);if(h.visible&&HudPresentation.enabled(h)&&h.type.equals("button")&&HudLayout.resolve(h,getWidth(),getHeight()).contains(event.getX(i),event.getY(i))){action=h.action.isBlank()?h.id:h.action;break;}}
                touchActions.put(event.getPointerId(i),action);
            }
            int up=(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_POINTER_UP)?event.getActionIndex():-1;
            if(up>=0)touchActions.remove(event.getPointerId(up));
            Set<String> next=new HashSet<>(touchActions.values());next.remove("");for(String action:held)if(!next.contains(action))runtime.setAction(action,false);for(String action:next)runtime.setAction(action,true);held.clear();held.addAll(next);
            dx=dy=0;
            for(int i=0;i<event.getPointerCount();i++)if(i!=up&&"".equals(touchActions.get(event.getPointerId(i)))){dx=event.getX(i)<getWidth()/3f?-1:event.getX(i)>getWidth()*2/3f?1:0;dy=event.getY(i)<getHeight()/3f?1:event.getY(i)>getHeight()*2/3f?-1:0;break;}
            return true;
        }
        void clearInput(){for(String action:held)runtime.setAction(action,false);held.clear();touchActions.clear();dx=dy=0;}
        void close(){active=false;runtime.close();if(music!=null){music.release();music=null;}disposed=true;imageLoader.shutdownNow();effects.release();samples.clear();streams.clear();images.clear();loadingImages.clear();failedImages.clear();}
    }
}
