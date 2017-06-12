import React from 'react';
import PageLayout from 'components/PageLayout';
import {DevicesSearch, DevicesList, Device, DevicesToolbar} from './components';
import {connect} from 'react-redux';
import _ from 'lodash';
import {List} from "immutable";

@connect((state) => ({
  devices: state.Devices.get('devices'),
}))
class Devices extends React.Component {

  static contextTypes = {
    router: React.PropTypes.object
  };

  static propTypes = {
    devices: React.PropTypes.instanceOf(List),
    location: React.PropTypes.object,
    params: React.PropTypes.object,
  };

  componentWillMount() {

    if (isNaN(Number(this.props.params.id)) || !this.getDeviceById(this.props.params.id)) {
      this.context.router.push('/devices/' + this.props.devices.first().get('id'));
    }
  }

  shouldComponentUpdate(nextProps) {
    return !(_.isEqual(nextProps.devices, this.props.devices)) || this.props.params.id !== nextProps.params.id || this.props.location.pathname !== nextProps.location.pathname;
  }

  handleDeviceSelect(device) {
    this.context.router.push(`/devices/${device.get('id')}`);
  }

  getDeviceById(id) {
    return this.props.devices.find(device => Number(device.get('id')) === Number(id));
  }

  render() {

    const selectedDevice = this.getDeviceById(this.props.params.id);

    if (!this.props.params.id || !selectedDevice)
      return null;

    return (
      <PageLayout>
        <PageLayout.Navigation>
          <DevicesSearch />
          <DevicesToolbar location={this.props.location} params={this.props.params}/>
          <DevicesList devices={this.props.devices} activeId={Number(this.props.params.id)}
                       onDeviceSelect={this.handleDeviceSelect.bind(this)}/>
        </PageLayout.Navigation>
        <PageLayout.Content>
          <Device device={selectedDevice}/>
        </PageLayout.Content>
      </PageLayout>
    );
  }

}

export default Devices;
