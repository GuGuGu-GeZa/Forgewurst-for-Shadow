package net.wurstclient.forge.ai;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;
import net.minecraft.client.Minecraft;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
public class AStarCustomPathFinder {
    private Vec3d startVec3;
    private Vec3d endVec3;
    private ArrayList<Vec3d> path = new ArrayList<Vec3d>();
    private ArrayList<Hub> hubs = new ArrayList<Hub>();
    private ArrayList<Hub> hubsToWork = new ArrayList<Hub>();
    private double minDistanceSquared = 9;
    private boolean nearest = true;

    private static Vec3d[] flatCardinalDirections = {
            new Vec3d(1, 0, 0),
            new Vec3d(-1, 0, 0),
            new Vec3d(0, 0, 1),
            new Vec3d(0, 0, -1)
    };

    public AStarCustomPathFinder(Vec3d startVec3, Vec3d endVec3) {
        this.startVec3 = startVec3.addVector(0, 0, 0);
        this.endVec3 = endVec3.addVector(0D, 0D, 0D);
    }

    public ArrayList<Vec3d> getPath() {
        return path;
    }

    public void compute() {
        compute(1000, 4);
    }
    public static boolean checkPositionValidity(Vec3d loc, boolean checkGround) {
        return checkPositionValidity((int) loc.x, (int) loc.y, (int) loc.z, checkGround);
    }

    public static boolean checkPositionValidity(int x, int y, int z, boolean checkGround) {
        BlockPos block1 = new BlockPos(x, y, z);
        BlockPos block2 = new BlockPos(x, y + 1, z);
        BlockPos block3 = new BlockPos(x, y - 1, z);
        return !isBlockSolid(block1) && !isBlockSolid(block2) && (isBlockSolid(block3) || !checkGround) && isSafeToWalkOn(block3);
    }
    private static boolean isSafeToWalkOn(BlockPos block) {
        return !(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockFence) && 
        		!(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockWall);
    }
    private static boolean isBlockSolid(BlockPos block) {
        return Minecraft.getMinecraft().world.getBlockState(block).isFullCube() ||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockSlab) ||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockStairs)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockCactus)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockChest)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockEnderChest)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockSkull)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockPane)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockFence)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockWall)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockGlass)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockPistonBase)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockPistonExtension)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockPistonMoving)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockStainedGlass)||
        		(Minecraft.getMinecraft().world.getBlockState(block) instanceof BlockTrapDoor);
    }


    public void compute(int loops, int depth) {
        path.clear();
        hubsToWork.clear();
        ArrayList<Vec3d> initPath = new ArrayList<Vec3d>();
        initPath.add(startVec3);
        hubsToWork.add(new Hub(startVec3, null, initPath, startVec3.squareDistanceTo(endVec3), 0, 0));
        search:
        for (int i = 0; i < loops; i++) {
            Collections.sort(hubsToWork, new CompareHub());
            int j = 0;
            if (hubsToWork.size() == 0) {
                break;
            }
            for (Hub hub : new ArrayList<Hub>(hubsToWork)) {
                j++;
                if (j > depth) {
                    break;
                } else {
                    hubsToWork.remove(hub);
                    hubs.add(hub);

                    for (Vec3d direction : flatCardinalDirections) {
                        Vec3d loc = hub.getLoc().add(direction);
                        if (checkPositionValidity(loc, false)) {
                            if (addHub(hub, loc, 0)) {
                                break search;
                            }
                        }
                    }

                    Vec3d loc1 = hub.getLoc().addVector(0, 1, 0);
                    if (checkPositionValidity(loc1, false)) {
                        if (addHub(hub, loc1, 0)) {
                            break search;
                        }
                    }

                    Vec3d loc2 = hub.getLoc().addVector(0, -1, 0);
                    if (checkPositionValidity(loc2, false)) {
                        if (addHub(hub, loc2, 0)) {
                            break search;
                        }
                    }
                }
            }
        }
        if (nearest) {
            Collections.sort(hubs, new CompareHub());
            path = hubs.get(0).getPath();
        }
    }

 

   

    public Hub isHubExisting(Vec3d loc) {
        for (Hub hub : hubs) {
            if (hub.getLoc().x == loc.x && hub.getLoc().y == loc.y && hub.getLoc().z == loc.z) {
                return hub;
            }
        }
        for (Hub hub : hubsToWork) {
            if (hub.getLoc().x == loc.x && hub.getLoc().y == loc.y && hub.getLoc().z == loc.z) {
                return hub;
            }
        }
        return null;
    }

    public boolean addHub(Hub parent, Vec3d loc, double cost) {
        Hub existingHub = isHubExisting(loc);
        double totalCost = cost;
        if (parent != null) {
            totalCost += parent.getTotalCost();
        }
        if (existingHub == null) {
            if ((( loc).x == endVec3.y && loc.y == endVec3.y && loc.y == endVec3.z) || (minDistanceSquared != 0 && loc.squareDistanceTo(endVec3) <= minDistanceSquared)) {
                path.clear();
                path = parent.getPath();
                path.add(loc);
                return true;
            } else {
                ArrayList<Vec3d> path = new ArrayList<Vec3d>(parent.getPath());
                path.add(loc);
                hubsToWork.add(new Hub(loc, parent, path, loc.squareDistanceTo(endVec3), cost, totalCost));
            }
        } else if (existingHub.getCost() > cost) {
            ArrayList<Vec3d> path = new ArrayList<Vec3d>(parent.getPath());
            path.add(loc);
            existingHub.setLoc(loc);
            existingHub.setParent(parent);
            existingHub.setPath(path);
            existingHub.setSquareDistanceToFromTarget(loc.squareDistanceTo(endVec3));
            existingHub.setCost(cost);
            existingHub.setTotalCost(totalCost);
        }
        return false;
    }

    private class Hub {
        private Vec3d loc = null;
        private Hub parent = null;
        private ArrayList<Vec3d> path;
        private double squareDistanceToFromTarget;
        private double cost;
        private double totalCost;

        public Hub(Vec3d loc, Hub parent, ArrayList<Vec3d> path, double squareDistanceToFromTarget, double cost, double totalCost) {
            this.loc = loc;
            this.parent = parent;
            this.path = path;
            this.squareDistanceToFromTarget = squareDistanceToFromTarget;
            this.cost = cost;
            this.totalCost = totalCost;
        }

        public Vec3d getLoc() {
            return loc;
        }

        public Hub getParent() {
            return parent;
        }

        public ArrayList<Vec3d> getPath() {
            return path;
        }

        public double getSquareDistanceToFromTarget() {
            return squareDistanceToFromTarget;
        }

        public double getCost() {
            return cost;
        }

        public void setLoc(Vec3d loc) {
            this.loc = loc;
        }

        public void setParent(Hub parent) {
            this.parent = parent;
        }

        public void setPath(ArrayList<Vec3d> path) {
            this.path = path;
        }

        public void setSquareDistanceToFromTarget(double squareDistanceToFromTarget) {
            this.squareDistanceToFromTarget = squareDistanceToFromTarget;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(double totalCost) {
            this.totalCost = totalCost;
        }
    }

    public class CompareHub implements Comparator<Hub> {
        @Override
        public int compare(Hub o1, Hub o2) {
            return (int) (
                    (o1.getSquareDistanceToFromTarget() + o1.getTotalCost()) - (o2.getSquareDistanceToFromTarget() + o2.getTotalCost())
            );
        }
    }
}
