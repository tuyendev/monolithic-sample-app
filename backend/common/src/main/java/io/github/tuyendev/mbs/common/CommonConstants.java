package io.github.tuyendev.mbs.common;

import java.util.List;

public abstract class CommonConstants {
	public static abstract class EntityName {
		public static final String ROLE = "roles";

		public static final String USER = "users";

		public static final String REFRESH_TOKEN = "refresh_tokens";

		public static final String ACCESS_TOKEN = "access_tokens";

		public static final String USER_ROLE = "user_roles";

		public static final String ROLE_AUTHORITY = "role_authorities";

		public static final String AUTHORITY = "authorities";

		public static final String GROUP = "groups";

		public static final String GROUP_MEMBER = "group_members";
	}

	public static abstract class EntityStatus {
		public static final Integer ACTIVE = 1;

		public static final Integer INACTIVE = 0;

		public static final Integer LOCK = 2;

		public static final Integer DELETED = 9;

		public static final Integer ENABLED = 1;

		public static final Integer DISABLED = 0;

		public static final Integer VERIFIED = 1;

		public static final Integer UNVERIFIED = 0;
	}


	public static abstract class TokenAudience {
		public static final String ACCESS_TOKEN = "ACCTN";

		public static final String REFRESH_TOKEN = "REFTN";
	}

	public static abstract class Role {
		public static final String DEFAULT_ROLE_MEMBER = "MEMBER";

		public static final String DEFAULT_ROLE_ADMIN = "ADMIN";
	}

	public static abstract class Privilege {

		public static final String READ_PREFIX = "READ_";

		public static final String WRITE_PREFIX = "WRITE_";

		public static final String UPDATE_PREFIX = "UPDATE_";

		public static final String DELETE_PREFIX = "DELETE_";

		public static final String READ_BASIC = "READ_BASIC";

		public static final String WRITE_BASIC = "WRITE_BASIC";

		public static final String UPDATE_BASIC = "UPDATE_BASIC";

		public static final String DELETE_BASIC = "DELETE_BASIC";

		public static final String READ_PRIVILEGE = "READ_PRIVILEGE";

		public static final String WRITE_PRIVILEGE = "WRITE_PRIVILEGE";

		public static final String UPDATE_PRIVILEGE = "UPDATE_PRIVILEGE";

		public static final String DELETE_PRIVILEGE = "DELETE_PRIVILEGE";

		public static final List<String> basisPrivileges = List.of(READ_BASIC, WRITE_BASIC, UPDATE_BASIC, DELETE_BASIC);

		public static final List<String> adminPrivileges = List.of(READ_PRIVILEGE, WRITE_PRIVILEGE, UPDATE_PRIVILEGE, DELETE_PRIVILEGE);

	}
}