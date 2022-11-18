package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Vec2d;

/**
 * {@link PortalState} but one-sided.
 * PortalState contains the information both this-side and other-side.
 * UnilateralPortalState is either the this-side state or other-side state.
 */
public record UnilateralPortalState(
    ResourceKey<Level> dimension,
    Vec3 position,
    DQuaternion orientation,
    double width,
    double height
) {
    // its inverse is itself
    public static final DQuaternion flipAxisH = DQuaternion.rotationByDegrees(
        new Vec3(1, 0, 0), 180
    ).fixFloatingPointErrorAccumulation();
    
    public static UnilateralPortalState extractThisSide(PortalState portalState) {
        return new UnilateralPortalState(
            portalState.fromWorld,
            portalState.fromPos,
            portalState.orientation,
            portalState.width,
            portalState.height
        );
    }
    
    public static UnilateralPortalState extractOtherSide(PortalState portalState) {
        DQuaternion otherSideOrientation = portalState.rotation
            .hamiltonProduct(portalState.orientation)
            .hamiltonProduct(flipAxisH);
        return new UnilateralPortalState(
            portalState.toWorld,
            portalState.toPos,
            otherSideOrientation,
            portalState.width * portalState.scaling,
            portalState.height * portalState.scaling
        );
    }
    
    public static PortalState combine(
        UnilateralPortalState thisSide,
        UnilateralPortalState otherSide
    ) {
        // otherSideOrientation * axis = rotation * thisSideOrientation * flipAxisH * axis
        // otherSideOrientation = rotation * thisSideOrientation * flipAxisH
        // rotation = otherSideOrientation * flipAxisH^-1 * thisSideOrientation^-1
        
        DQuaternion rotation = otherSide.orientation
            .hamiltonProduct(flipAxisH)
            .hamiltonProduct(thisSide.orientation.getConjugated());
        
        double scale = otherSide.width / thisSide.width;
        // ignore other side's aspect ratio changing
        
        PortalState result = new PortalState(
            thisSide.dimension,
            thisSide.position,
            otherSide.dimension,
            otherSide.position,
            scale,
            rotation,
            thisSide.orientation,
            thisSide.width,
            thisSide.height
        );
        
        return result;
    }
    
    public static UnilateralPortalState interpolate(
        UnilateralPortalState from,
        UnilateralPortalState to,
        double progress
    ) {
        return new UnilateralPortalState(
            from.dimension,
            Helper.interpolatePos(from.position, to.position, progress),
            DQuaternion.interpolate(from.orientation, to.orientation, progress),
            Mth.lerp(progress, from.width, to.width),
            Mth.lerp(progress, from.height, to.height)
        );
    }
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.location().toString());
        Helper.putVec3d(tag, "position", position);
        tag.put("orientation", orientation.toTag());
        tag.putDouble("width", width);
        tag.putDouble("height", height);
        return tag;
    }
    
    public static UnilateralPortalState fromTag(CompoundTag tag) {
        ResourceKey<Level> dimension = DimId.idToKey(tag.getString("dimension"));
        Vec3 point = Helper.getVec3d(tag, "position");
        DQuaternion orientation = DQuaternion.fromTag(tag.getCompound("orientation"));
        double width = tag.getDouble("width");
        double height = tag.getDouble("height");
        return new UnilateralPortalState(
            dimension, point, orientation, width, height
        );
    }
    
    public DeltaUnilateralPortalState subtract(UnilateralPortalState other) {
        Vec3 offset = position.subtract(other.position);
        DQuaternion rotation = orientation.hamiltonProduct(other.orientation.getConjugated());
        double widthScale = width / other.width;
        double heightScale = height / other.height;
        return new DeltaUnilateralPortalState(
            offset,
            rotation,
            new Vec2d(widthScale, heightScale)
        ).purgeFPError();
    }
    
    public UnilateralPortalState apply(DeltaUnilateralPortalState thisSideDelta) {
        return new Builder().from(this).apply(thisSideDelta).build();
    }
    
    /**
     * A mutable version of {@link UnilateralPortalState}.
     * Generated by GitHub Copilot.
     */
    public static class Builder {
        public ResourceKey<Level> dimension;
        public Vec3 position;
        public DQuaternion orientation;
        public double width;
        public double height;
        
        public UnilateralPortalState build() {
            return new UnilateralPortalState(
                dimension,
                position,
                orientation,
                width,
                height
            );
        }
        
        public Builder dimension(ResourceKey<Level> dimension) {
            this.dimension = dimension;
            return this;
        }
        
        public Builder position(Vec3 point) {
            this.position = point;
            return this;
        }
        
        public Builder orientation(DQuaternion orientation) {
            this.orientation = orientation;
            return this;
        }
        
        public Builder width(double width) {
            this.width = width;
            return this;
        }
        
        public Builder height(double height) {
            this.height = height;
            return this;
        }
        
        @NotNull
        public Builder from(UnilateralPortalState other) {
            this.dimension = other.dimension;
            this.position = other.position;
            this.orientation = other.orientation;
            this.width = other.width;
            this.height = other.height;
            return this;
        }
        
        public Builder offset(Vec3 offset) {
            this.position = this.position.add(offset);
            return this;
        }
        
        public Builder rotate(DQuaternion rotation) {
            this.orientation = rotation.hamiltonProduct(this.orientation);
            return this;
        }
        
        public Builder scaleWidth(double scale) {
            this.width *= scale;
            return this;
        }
        
        public Builder scaleHeight(double scale) {
            this.height *= scale;
            return this;
        }
        
        public Builder apply(DeltaUnilateralPortalState delta) {
            if (delta.offset() != null) {
                this.position = this.position.add(delta.offset());
            }
            if (delta.rotation() != null) {
                this.orientation = delta.rotation().hamiltonProduct(this.orientation);
            }
            if (delta.sizeScaling() != null) {
                this.width *= delta.sizeScaling().x();
                this.height *= delta.sizeScaling().y();
            }
            return this;
        }
        
        @Deprecated
        @NotNull
        public Builder correctFrom(UnilateralPortalState other) {
            this.dimension = other.dimension;
            if (position.distanceToSqr(other.position) > 0.0001) {
                this.position = other.position;
            }
            if (!DQuaternion.isClose(orientation, other.orientation, 0.001)) {
                this.orientation = other.orientation;
            }
            if (Math.abs(width - other.width) > 0.0001) {
                this.width = other.width;
            }
            if (Math.abs(height - other.height) > 0.0001) {
                this.height = other.height;
            }
            
            return this;
        }
    }
}
