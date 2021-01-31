package com.github.ruediste.laserPcb.profile;

import java.util.UUID;

public class ProfileList {
	public ProfileList(Profile profile) {
		id = profile.id;
		name = profile.name;
	}

	public UUID id;
	public String name;
}
