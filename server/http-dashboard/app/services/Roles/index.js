const Roles = {
  'SUPER_ADMIN': {
    value: 'SUPER_ADMIN',
    title: 'Super Admin'
  },
  'ADMIN': {
    value: 'ADMIN',
    title: 'Admin'
  },
  'STAFF': {
    value: 'STAFF',
    title: 'Staff'
  },
  'USER': {
    value: 'USER',
    title: 'User'
  }
};

const InviteAvailableRoles = [
  Roles.ADMIN,
  Roles.STAFF,
  Roles.USER
];

const UsersAvailableRoles = [
  Roles.ADMIN,
  Roles.STAFF,
  Roles.USER
];

const MetadataRoles = [
  {
    key: Roles.ADMIN.value,
    value: Roles.ADMIN.title
  },
  {
    key: Roles.STAFF.value,
    value: Roles.STAFF.title
  },
  {
    key: Roles.USER.value,
    value: Roles.USER.title
  }
];

const MetadataRolesDefault = MetadataRoles[0].key;

export {
  MetadataRoles,
  MetadataRolesDefault,
  Roles,
  InviteAvailableRoles,
  UsersAvailableRoles
};
