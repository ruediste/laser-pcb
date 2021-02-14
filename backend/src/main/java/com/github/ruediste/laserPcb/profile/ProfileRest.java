package com.github.ruediste.laserPcb.profile;

import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileRest {
	private final Logger logger = LoggerFactory.getLogger(ProfileRest.class);

	@Autowired
	ProfileRepository repo;

	@Autowired
	CurrentProfileIdRepository currentProfileIdRepository;

	@GetMapping("profile")
	List<ProfileList> getProfiles() {
		return repo.getAll().stream().sorted(Comparator.comparing(x -> x.name == null ? "" : x.name))
				.map(x -> new ProfileList(x)).collect(toList());
	}

	@PostMapping("profile")
	ProfileList createProfile(@RequestBody Profile profile) {
		repo.save(profile);
		return new ProfileList(profile);
	}

	@PostMapping("profile/_reload")
	void reloadProfiles() {
		repo.reload();
	}

	@PostMapping("profile/current")
	void setCurrentProfile(@RequestParam("id") String id) {
		currentProfileIdRepository.set(id == null ? null : new CurrentProfileId(UUID.fromString(id)));
	}

	@GetMapping("profile/current")
	ProfileList getCurrentProfile() {
		CurrentProfileId currentProfileId = currentProfileIdRepository.get();
		return currentProfileId == null ? null : new ProfileList(repo.get(currentProfileId.id));
	}

	@DeleteMapping("profile/{id}")
	void deleteProfile(@PathVariable UUID id) {
		repo.delete(id);
	}

	@GetMapping("profile/{id}")
	Profile getProfile(@PathVariable UUID id) {
		return repo.get(id);
	}

	@PostMapping("profile/{id}")
	void saveProfile(@PathVariable UUID id, @RequestBody Profile profile) {
		profile.id = id;
		repo.save(profile);
	}

	@PostMapping("profile/{id}/_copy")
	ProfileList copyProfile(@PathVariable UUID id) {
		Profile profile = repo.get(id);
		profile.name = "Copy of " + profile.name;
		profile.id = null;
		repo.save(profile);
		return new ProfileList(profile);
	}

}
