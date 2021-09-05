package com.github.ruediste.laserPcb.process.printPcb;

public enum Corner {
	BL {
		@Override
		public Corner opposite() {
			return BR;
		}
	},
	BR {
		@Override
		public Corner opposite() {
			return BL;
		}
	};

	public abstract Corner opposite();
}
