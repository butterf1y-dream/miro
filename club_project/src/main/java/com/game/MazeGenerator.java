package com.game;

import java.awt.Point;
import java.util.*;

public class MazeGenerator {

    private static List<Point> escapePath;

    public static int[][] generate(int w,int h){
        int[][] m=new int[h][w];
        for(int y=0;y<h;y++)
            for(int x=0;x<w;x++) m[y][x]=1;

        carve(1,1,m);
        m[h-2][w-2]=2;

        escapePath=findPath(m,1,1,w-2,h-2);
        return m;
    }

    public static List<Point> getEscapePath(){ return escapePath; }

    private static void carve(int x,int y,int[][] m){
        int[] d={0,1,2,3};
        shuffle(d);
        m[y][x]=0;
        for(int i:d){
            int dx=(i==0?1:i==1?-1:0)*2;
            int dy=(i==2?1:i==3?-1:0)*2;
            int nx=x+dx, ny=y+dy;
            if(ny>0&&nx>0&&ny<m.length-1&&nx<m[0].length-1&&m[ny][nx]==1){
                m[y+dy/2][x+dx/2]=0;
                carve(nx,ny,m);
            }
        }
    }

    private static List<Point> findPath(int[][] m,int sx,int sy,int ex,int ey){
        int h=m.length,w=m[0].length;
        boolean[][] v=new boolean[h][w];
        Point[][] p=new Point[h][w];
        Queue<Point> q=new ArrayDeque<>();
        q.add(new Point(sx,sy)); v[sy][sx]=true;

        int[] dx={1,-1,0,0}, dy={0,0,1,-1};

        while(!q.isEmpty()){
            Point c=q.poll();
            if(c.x==ex&&c.y==ey) break;
            for(int i=0;i<4;i++){
                int nx=c.x+dx[i], ny=c.y+dy[i];
                if(nx<0||ny<0||nx>=w||ny>=h||v[ny][nx]||m[ny][nx]==1) continue;
                v[ny][nx]=true; p[ny][nx]=c;
                q.add(new Point(nx,ny));
            }
        }

        List<Point> path=new ArrayList<>();
        for(Point cur=new Point(ex,ey);cur!=null;cur=p[cur.y][cur.x])
            path.add(cur);
        Collections.reverse(path);
        return path;
    }

    private static void shuffle(int[] a){
        Random r=new Random();
        for(int i=0;i<a.length;i++){
            int j=r.nextInt(a.length);
            int t=a[i]; a[i]=a[j]; a[j]=t;
        }
    }
}
