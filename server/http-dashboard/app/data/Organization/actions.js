export function OrganizationFetch(data = {}) {
  if (!data.id)
    throw Error('Organization id is not specified');
  return {
    type: 'API_ORGANIZATION',
    payload: {
      request: {
        method: 'get',
        url: `/organization/${data.id}`
      }
    }
  };
}

export function OrganizationSave(data = {}) {
  if (!data.id)
    throw Error('Organization id is not specified');
  return {
    type: 'API_ORGANIZATION_SAVE',
    payload: {
      request: {
        method: 'post',
        url: `/organization/${data.id}`,
        data: data
      }
    }
  };
}

export function OrganizationSendInvite(data = {}) {
  if (!data.id)
    throw Error('Organization id is not specified');
  return {
    type: 'API_ORGANIZATION_SEND_INVITE',
    payload: {
      request: {
        method: 'post',
        url: `/organization/${data.id}/invite`,
        data: data
      }
    }
  };
}

export function OrganizationUpdateName(name) {
  return {
    type: 'ORGANIZATION_UPDATE_NAME',
    name: name
  };
}

export function OrganizationBrandingUpdate(colors) {
  return {
    type: 'ORGANIZATION_BRANDING_UPDATE',
    colors
  };
}

export function OrganizationUpdateTimezone(tzName) {
  return {
    type: 'ORGANIZATION_UPDATE_TIMEZONE',
    tzName: tzName
  };
}
