package jadx.utils;

import jadx.dex.attributes.AttributeType;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;
import jadx.dex.trycatch.CatchAttr;
import jadx.dex.trycatch.ExceptionHandler;
import jadx.dex.trycatch.TryCatchBlock;
import jadx.utils.exceptions.JadxRuntimeException;

import java.util.List;

public class RegionUtils {

	public static boolean hasExitEdge(IContainer container) {
		if (container instanceof BlockNode) {
			return ((BlockNode) container).getSuccessors().size() != 0;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			List<IContainer> blocks = region.getSubBlocks();
			if (blocks.isEmpty())
				return false;
			return hasExitEdge(blocks.get(blocks.size() - 1));
		} else {
			throw new JadxRuntimeException("Unknown container type: " + container.getClass());
		}
	}

	public static boolean notEmpty(IContainer container) {
		if (container instanceof BlockNode) {
			return ((BlockNode) container).getInstructions().size() != 0;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer block : region.getSubBlocks()) {
				if (notEmpty(block))
					return true;
			}
			return false;
		} else {
			throw new JadxRuntimeException("Unknown container type: " + container.getClass());
		}
	}

	public static void getAllRegionBlocks(IContainer container, List<BlockNode> blocks) {
		if (container instanceof BlockNode) {
			blocks.add((BlockNode) container);
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer block : region.getSubBlocks()) {
				getAllRegionBlocks(block, blocks);
			}
		} else {
			throw new JadxRuntimeException("Unknown container type: " + container.getClass());
		}
	}

	public static boolean isRegionContainsBlock(IContainer container, BlockNode block) {
		if (container instanceof BlockNode) {
			return container == block;
		} else if (container instanceof IRegion) {
			IRegion region = (IRegion) container;
			for (IContainer b : region.getSubBlocks()) {
				if (isRegionContainsBlock(b, block))
					return true;
			}
			return false;
		} else {
			throw new JadxRuntimeException("Unknown container type: " + container.getClass());
		}
	}

	private static boolean isRegionContainsExcHandlerRegion(IContainer container, IRegion region) {
		if (container == region)
			return true;

		if (container instanceof IRegion) {
			IRegion r = (IRegion) container;

			// process sub blocks
			for (IContainer b : r.getSubBlocks()) {
				// process try block
				CatchAttr cb = (CatchAttr) b.getAttributes().get(AttributeType.CATCH_BLOCK);
				if (cb != null && (b instanceof IRegion)) {
					TryCatchBlock tb = cb.getTryBlock();
					for (ExceptionHandler eh : tb.getHandlers()) {
						if (isRegionContainsRegion(eh.getHandlerRegion(), region))
							return true;
					}
					if (tb.getFinalBlock() != null) {
						if (isRegionContainsRegion(tb.getFinalBlock(), region))
							return true;
					}
				}
				if (isRegionContainsRegion(b, region))
					return true;
			}
		}
		return false;
	}

	/**
	 * Check if region contains in container
	 * 
	 * For simple region (not from exception handlers) search in parents
	 * otherwise run recursive search because exception handlers can have several parents
	 */
	public static boolean isRegionContainsRegion(IContainer container, IRegion region) {
		if (container == region) return true;
		if (region == null) return false;

		IRegion parent = region.getParent();
		while (container != parent) {
			if (parent == null) {
				if (region.getAttributes().contains(AttributeType.EXC_HANDLER))
					return isRegionContainsExcHandlerRegion(container, region);
				else
					return false;
			}
			region = parent;
			parent = region.getParent();
		}
		return true;
	}

	public static boolean isDominaterBy(BlockNode dom, IContainer cont) {
		assert cont != null;

		if (dom == cont)
			return true;

		if (cont instanceof BlockNode) {
			BlockNode block = (BlockNode) cont;
			return block.isDominator(dom);
		} else if (cont instanceof IRegion) {
			IRegion region = (IRegion) cont;
			for (IContainer c : region.getSubBlocks()) {
				if (!isDominaterBy(dom, c)) {
					return false;
				}
			}
			return true;
		} else {
			throw new JadxRuntimeException("Unknown container type: " + cont.getClass());
		}
	}

	public static boolean hasPathThruBlock(BlockNode block, IContainer cont) {
		if (block == cont)
			return true;

		if (cont instanceof BlockNode) {
			return BlockUtils.isPathExists(block, (BlockNode) cont);
		} else if (cont instanceof IRegion) {
			IRegion region = (IRegion) cont;
			for (IContainer c : region.getSubBlocks()) {
				if (!hasPathThruBlock(block, c))
					return false;
			}
			return true;
		} else {
			throw new JadxRuntimeException("Unknown container type: " + cont.getClass());
		}
	}

}
