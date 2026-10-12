package megalodonte.engine;
import java.util.List;
/** Polygon versus axis-aligned actor bounds. Touching alone is not penetration. */
public final class CollisionGeometry {
    private CollisionGeometry(){}
    public static void validate(List<Double> p){
        if(p==null||(!p.isEmpty()&&(p.size()<6||p.size()%2!=0||p.size()>256)))throw new IllegalArgumentException("Polígono de colisão inválido");
        for(Double v:p)if(v==null||!Double.isFinite(v))throw new IllegalArgumentException("Vértice inválido");
        if(!p.isEmpty()){double area=0;for(int i=0,j=p.size()-2;i<p.size();j=i,i+=2)area+=p.get(j)*p.get(i+1)-p.get(i)*p.get(j+1);if(Math.abs(area)<1e-10)throw new IllegalArgumentException("Polígono sem área");}
    }
    public static boolean intersects(Runtime.CollisionRect r,List<Double> p,double ox,double oy,double scale){
        if(p.isEmpty())return true;
        double left=(r.x()-ox)/scale,right=(r.x()+r.width()-ox)/scale,bottom=(r.y()-oy)/scale,top=(r.y()+r.height()-oy)/scale,eps=1e-7;
        for(int i=0;i<p.size();i+=2)if(p.get(i)>left+eps&&p.get(i)<right-eps&&p.get(i+1)>bottom+eps&&p.get(i+1)<top-eps)return true;
        double[] xs={left+eps,right-eps,right-eps,left+eps},ys={bottom+eps,bottom+eps,top-eps,top-eps};
        for(int k=0;k<4;k++)if(inside(xs[k],ys[k],p))return true;
        for(int i=0,j=p.size()-2;i<p.size();j=i,i+=2)for(int k=0;k<4;k++){
            int n=(k+1)%4;if(crosses(p.get(j),p.get(j+1),p.get(i),p.get(i+1),xs[k],ys[k],xs[n],ys[n]))return true;
        }
        return false;
    }
    private static boolean inside(double x,double y,List<Double> p){boolean in=false;for(int i=0,j=p.size()-2;i<p.size();j=i,i+=2){double xi=p.get(i),yi=p.get(i+1),xj=p.get(j),yj=p.get(j+1);if((yi>y)!=(yj>y)&&x<(xj-xi)*(y-yi)/(yj-yi)+xi)in=!in;}return in;}
    private static double side(double ax,double ay,double bx,double by,double x,double y){return (bx-ax)*(y-ay)-(by-ay)*(x-ax);}
    private static boolean crosses(double ax,double ay,double bx,double by,double cx,double cy,double dx,double dy){return side(ax,ay,bx,by,cx,cy)*side(ax,ay,bx,by,dx,dy)<0&&side(cx,cy,dx,dy,ax,ay)*side(cx,cy,dx,dy,bx,by)<0;}
}
