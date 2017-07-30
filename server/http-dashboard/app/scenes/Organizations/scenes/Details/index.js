import React                from 'react';
import {MainLayout}         from 'components';
import {connect}            from 'react-redux';
import {ProductsFetch}      from 'data/Product/api';
import {
  Button,
  Tabs,
  message,
  Popconfirm
}                           from 'antd';
import PropTypes            from 'prop-types';
import {
  Map,
  List,
  fromJS
}                           from 'immutable';
import {bindActionCreators} from 'redux';
import {reset}              from 'redux-form';
import {
  OrganizationsDetailsUpdate,
  OrganizationsFetch,
  OrganizationsUsersFetch,
  OrganizationsDelete,
}                           from 'data/Organizations/actions';

import {
  OrganizationUsersDelete,
  OrganizationSendInvite
}                           from 'data/Organization/actions';

import {
  Info,
  Products
}                           from './components';

import AdminsEditScene from "../AdminsEdit";

const {TabPane} = Tabs;

import './styles.less';

@connect((state) => ({
  list: state.Organizations.get('list') || null,
  details: state.Organizations.get('details'),
  admins: state.Organizations.get('adminsEdit') || null,
}), (dispatch) => ({
  resetForm: bindActionCreators(reset, dispatch),
  fetchProducts: bindActionCreators(ProductsFetch, dispatch),
  OrganizationsFetch: bindActionCreators(OrganizationsFetch, dispatch),
  OrganizationsDelete: bindActionCreators(OrganizationsDelete, dispatch),
  OrganizationSendInvite: bindActionCreators(OrganizationSendInvite, dispatch),
  OrganizationsUsersFetch: bindActionCreators(OrganizationsUsersFetch, dispatch),
  OrganizationUsersDelete: bindActionCreators(OrganizationUsersDelete, dispatch),
  OrganizationsDetailsUpdate: bindActionCreators(OrganizationsDetailsUpdate, dispatch),
}))
class Details extends React.Component {

  static contextTypes = {
    router: PropTypes.object
  };

  static propTypes = {
    list: PropTypes.instanceOf(List),
    admins: PropTypes.instanceOf(Map),
    details: PropTypes.instanceOf(Map),

    params: PropTypes.object,

    resetForm: PropTypes.func,
    fetchProducts: PropTypes.func,
    OrganizationsFetch: PropTypes.func,
    OrganizationsDelete: PropTypes.func,
    OrganizationSendInvite: PropTypes.func,
    OrganizationsUsersFetch: PropTypes.func,
    OrganizationUsersDelete: PropTypes.func,
    OrganizationsDetailsUpdate: PropTypes.func,
  };

  constructor(props) {
    super(props);

    this.handleTabChange = this.handleTabChange.bind(this);
    this.handleOrganizationEdit = this.handleOrganizationEdit.bind(this);
    this.handleOrganizationDelete = this.handleOrganizationDelete.bind(this);
  }

  componentWillMount() {

    const redirectIfNotExist = (list) => {
      if (!list.find(org => org.get('id') === Number(this.props.params.id)))
        this.context.router.push('/organizations/?notFound=true');
    };

    if (!this.props.list) {
      this.props.OrganizationsFetch().then((response) => {
        redirectIfNotExist(fromJS(response.payload.data));
      });
    }

    if (!this.props.details.get('users')) {
      this.props.OrganizationsUsersFetch({
        id: this.props.params.id
      });
    }

    if (this.props.list)
      redirectIfNotExist(this.props.list);

  }

  componentWillUnmount() {
    this.props.OrganizationsDetailsUpdate(
      this.props.details
        .set('activeTab', this.TABS.INFO)
        .set('users', null)
    );
  }

  handleOrganizationEdit() {

    this.props.OrganizationsDetailsUpdate(this.props.details.set('loading', true));

    return Promise.all([
      this.props.OrganizationsFetch(),
      this.props.fetchProducts(),
      this.props.OrganizationsUsersFetch({id: this.props.params.id})
    ]).then(() => {
      setTimeout(() => {
        this.props.OrganizationsDetailsUpdate(this.props.details.set('loading', false));
        this.context.router.push(`/organizations/edit/${this.props.params.id}`);
      }, 500);
    });
  }


  TABS = {
    INFO: 'info',
    PRODUCTS: 'products',
    ADMINS: 'admins'
  };

  toggleOrganizationDeleteLoading(state) {
    this.props.OrganizationsDetailsUpdate(
      this.props.details.set('organizationDeleteLoading', state)
    );
  }

  handleOrganizationDelete() {
    this.toggleOrganizationDeleteLoading(true);

    this.props.OrganizationsDelete({
      id: this.props.params.id
    }).then(() => {

      this.props.OrganizationsFetch().then(() => {

        this.context.router.push('/organizations');

        this.toggleOrganizationDeleteLoading(false);
      });

    }).catch((response) => {
      this.toggleOrganizationDeleteLoading(false);

      const data = response.error.response.data;

      message.error(data.error && data.error.message || 'Cannot delete organization');
    });
  }

  handleTabChange(tab) {
    this.props.OrganizationsDetailsUpdate(
      this.props.details.set('activeTab', tab)
    );
  }

  render() {

    if (!this.props.list)
      return null;

    if (!this.props.admins.get('users'))
      return null;

    const organization = this.props.list.find(org => org.get('id') === Number(this.props.params.id));

    if (!organization)
      return null;

    return (
      <MainLayout>
        <MainLayout.Header title={organization.get('name')}
                           options={(
                             <div>
                               <Popconfirm title="Are you sure？" okText="Yes" cancelText="No"
                                           onConfirm={this.handleOrganizationDelete}>
                                 <Button type="danger">Delete</Button>
                               </Popconfirm>
                               <Button type="primary" loading={this.props.details.get('loading')}
                                       onClick={this.handleOrganizationEdit}>Edit</Button>
                             </div>
                           )}/>
        <MainLayout.Content className="product-details-content">
          <Tabs onChange={this.handleTabChange}
                activeKey={this.props.details.get('activeTab')}>
            <TabPane tab="Info"
                     key={this.TABS.INFO}>
              <div className="organizations-manage-tab-wrapper">
                <Info name={organization.get('name')}
                      description={organization.get('description')}
                      logoUrl={organization.get('logoUrl')}
                      canCreateOrgs={organization.get('canCreateOrgs')}/>
              </div>
            </TabPane>
            <TabPane tab="Products"
                     key={this.TABS.PRODUCTS}>
              <div className="organizations-manage-tab-wrapper">
                <Products products={organization.get('products')}/>
              </div>
            </TabPane>
            <TabPane tab="Admins"
                     key={this.TABS.ADMINS}>
              <div className="organizations-manage-tab-wrapper">
                <AdminsEditScene params={this.props.params}/>
              </div>
            </TabPane>
          </Tabs>
        </MainLayout.Content>
      </MainLayout>
    );
  }

}

export default Details;
