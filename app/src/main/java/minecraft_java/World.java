package minecraft_java;

import java.util.HashMap;
import java.util.Map;

public class World {
    private HashMap<Key, Chunk> chunks;
    private HashMap<Key, Chunk> loadedChunks;
    private HashMap<Key, Chunk> unloadedChunks;
    private int chunkSize = 16;
    private int chunkHeight = 64;
    private int renderDistance = 3;

    public World(int chunkSize) {
        this.chunkSize = chunkSize;
        loadedChunks = new HashMap<>();
        unloadedChunks = new HashMap<>();
        chunks = new HashMap<>();
    }

    public int getSize(){
        return loadedChunks.size();
    }
    public Chunk getChunk(Key k){
        return loadedChunks.get(k);
    }

    public void draw(){
        loadedChunks.values().forEach((Chunk c) -> c.draw());
    }

    private Key oldPlayerChunk;
    public void updateChunks(Player p){
        Key playerChunk = getPlayerChunk(p);
        //System.out.println(p.getPos().x / chunkSize + " : " + p.getPos().z / chunkSize);
        if(oldPlayerChunk == null || !oldPlayerChunk.equals(playerChunk)){
            oldPlayerChunk = playerChunk;
            loadNewChunks(p);
        }
    }

    public int getBlock(Key k, int x, int y, int z){
        Chunk c = chunks.get(k);
        if(c != null) return c.getBlocks()[x][y][z];
        return -1;
    }

    private Key getPlayerChunk(Player p){
        float x = p.getPos().x, z = p.getPos().z;
        x = x + x/Math.abs(x) * chunkSize/2;
        z = z + z/Math.abs(z) * chunkSize/2;

        return new Key((int) x/chunkSize, (int) z/chunkSize);
    }

    private float distanceToPlayer(Key k, Player p) {
        Key pk = k.subtract(getPlayerChunk(p));
        double x = (double) pk.x;
        double z = (double) pk.z;
        return (float) Math.pow((Math.pow(x, 2)+Math.pow(z, 2)), 0.5);
    }


    private void loadNewChunks(Player p){
        printDebugData();
        //float fov = p.getCam().getFov();
        Key pk = getPlayerChunk(p);
        HashMap<Key, Chunk> toUnload = new HashMap<Key, Chunk>(loadedChunks);
        //om new Key inte existerar put(new chunk)
        for (int i = -(renderDistance+1); i <= renderDistance+1; i++) {
            for (int j = -(renderDistance+1); j <= renderDistance+1; j++) {
                Key k = new Key(pk.x + i, pk.z + j);
                //Not loaded but should be
                if (!(loadedChunks.containsKey(k)) && distanceToPlayer(k, p) <= renderDistance){
                    if(unloadedChunks.containsKey(k)){
                        //System.out.println("Loading chunk from memory " + k.toString());
                        System.out.println(k);
                        loadedChunks.put(k, unloadedChunks.get(k));
                        unloadedChunks.remove(k);
                    }
                    else{
                        //System.out.println("Generating new chunk " + k.toString());
                        Chunk chunk = TerrainGenerator.generateChunk(k, chunkSize, chunkHeight);
                        loadedChunks.put(k, chunk);
                        chunks.put(k, chunk);
                        //System.out.println("loadedChunks: " + getSize());
                    }
                } else if (!loadedChunks.containsKey(k) && distanceToPlayer(k, p) > renderDistance){
                    Chunk chunk = TerrainGenerator.generateChunk(k, chunkSize, chunkHeight);
                    chunks.put(k, chunk);
                } else if (!(distanceToPlayer(k, p) > renderDistance)){ //loaded and should be
                    toUnload.remove(k);
                }
            }
        }
        //loadedChunks.keySet().forEach(k -> System.out.println(k));
        unloadedChunks.putAll(toUnload);
        toUnload.keySet().forEach(key -> loadedChunks.remove(key));
        //toUnload.keySet().forEach(key -> System.out.println("Unloading chunk " + key.toString()));
    
        //Load unloaded MeshMaps
        for(Map.Entry<Key, Chunk> entry : loadedChunks.entrySet()){
            //Key k = entry.getKey();
            Chunk c = entry.getValue();
            if (!c.hasMesh()){
                c.updateMesh();
            }
        }

    }

    public void printDebugData() {
        System.out.println("\nCurrently loaded: " + getSize());
        System.out.println("Total generated: " + (getSize() + unloadedChunks.size()));
        System.out.println("Size in ram estimate:\nBlockdata: "
                + (getSize() + unloadedChunks.size()) * 16 * 16 * 64 * 4 / 1E6 + "MB\n" + "Meshdata: "
                + (getSize() + unloadedChunks.size()) * 500 * (12 + 16 * 16 * 3) * 4 / 1E6 + "MB");
    }
}
