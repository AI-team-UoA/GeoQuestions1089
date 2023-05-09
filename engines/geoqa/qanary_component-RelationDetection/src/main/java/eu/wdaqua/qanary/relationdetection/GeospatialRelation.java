package eu.wdaqua.qanary.relationdetection;

/**
 * a type-safe representation of all supported geospatial relations and their textual representations
 * 
 * @author AnBo
 *
 */
public enum GeospatialRelation {
	// TODO: check the demand for multilingual support
	IN {
		@Override
		public String[] getLabels() {
			return new String[]{"in", "within","of","into","inside", "includes","located","belong"};
		}
		@Override
		public String getURI() {
			return new String("geof:sfWithin");
		}
	}, CONTAINS {
		@Override
		public String[] getLabels() {
			return new String[]{ "contain","include","have","has"};
		}
		@Override
		public String getURI() {
			return new String("geof:sfContains");
		}
	},NORTH_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"above","north","northern"};
		}
		@Override
		public String getURI() {
			return new String("strdf:above");
		}
	}, NORTH_WEST_OF {
		@Override
		public String[] getLabels() {
			return new String[]{ "aboveleft","leftabove","northwest","westnorth"};
		}
		@Override
		public String getURI() {
			return new String("strdf:above_left");
		}
	}, NORTH_EAST_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"eastnorth","aboveright", "rightabove", "northeast"};
		}
		@Override
		public String getURI() {
			return new String("strdf:above_right");
		}
	}, SOUTH_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"south", "below","southern"};
		}
		@Override
		public String getURI() {
			return new String("strdf:below");
		}
	}, SOUTH_WEST_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"westsouth","belowleft", "leftbelow", "southwest"};
		}
		@Override
		public String getURI() {
			return new String("strdf:below_left");
		}
	}, SOUTH_EAST_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"eastsouth","belowright", "rightbelow","southeast"};
		}
		@Override
		public String getURI() {
			return new String("strdf:below_right");
		}
	}, WEST_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"west","left","western"};
		}
		@Override
		public String getURI() {
			return new String("strdf:left");
		}
	}, EAST_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"east","right","eastern"};
		}
		@Override
		public String getURI() {
			return new String("strdf:right");
		}
	}, NEAR_BY {
		@Override
		public String[] getLabels() {
			return new String[]{"near","nearby", "close", "at most","around","less than","at least","nearest","closest","distance","far"};
		}
		@Override
		public String getURI() {
			return new String("geof:distance");
		}
	}, IN_THE_CENTER_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"center",  "middle"};
		}
		@Override
		public String getURI() {
			return new String("postgis:ST_Centroid");
		}
	}, AT_THE_BORDER_OF {
		@Override
		public String[] getLabels() {
			return new String[]{"border", "outskirts","boundary","surround","adjacent","next","touch"};
		}
		@Override
		public String getURI() {
			return new String("geof:boundary");
		}
	}, INTERSECT {
		@Override
		public String[] getLabels() {
			return new String[]{"intersect","overlap"};
		}
		@Override
		public String getURI() {
			return new String("geof:sfIntersect");
		}
	}, CROSSES {
		@Override
		public String[] getLabels() {
			return new String[]{"crosses", "cross","flows","flow","on","pass"};
		}
		@Override
		public String getURI() {
			return new String("geof:sfCrosses");
		}
	};
	
    public abstract String[] getLabels();
    public abstract String getURI();
}
