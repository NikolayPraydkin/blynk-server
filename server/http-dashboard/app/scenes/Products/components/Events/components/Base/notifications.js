import React from 'react';
import {Switch, Select} from 'antd';
import {Field} from 'redux-form';
import FormItem from 'components/FormItem';
import {Item} from 'components/UI';
import Static from './notifications-static';
import {List} from 'immutable';
import PropTypes from 'prop-types';

class Notifications extends React.Component {

  static propTypes = {
    contactMetaFields: PropTypes.instanceOf(List),
    field: PropTypes.object,
    onFocus: PropTypes.func,
    onBlur: PropTypes.func,
  };

  constructor(props) {
    super(props);

    this.notificationSelect = this.notificationSelect.bind(this);
  }

  notificationSelect(props) {

    const getValue = () => {

      if(!Array.isArray(props.input.value))
        return [];

      return props.input.value.filter((id) => (
        this.props.contactMetaFields.some((field) => Number(id) === Number(field.get('id')))
      ));
    };

    const onChange = (value) => {
      props.input.onChange(value);
    };

    const options = this.props.contactMetaFields.map((field) => {

      return (
        <Select.Option key={`${field.get('id')}`}
                       value={`${field.get('id')}`}>
          {field.get('name')}
        </Select.Option>
      );
    });

    return (

      <Select mode="tags"
              style={{width: '100%'}}
              onFocus={props.input.onFocus}
              onBlur={props.input.onBlur}
              value={getValue()}
              onChange={onChange}
              allowClear={true}
              placeholder="Select contact"
              notFoundContent={!options.length ? 'Add a "Contact" type Metadata to enable notifications' : 'No field matches your request'}>
        {options.toJS()}
      </Select>

    );

    // return (
    //   <Select mode="tags"
    //           onFocus={props.input.onFocus}
    //           onBlur={props.input.onBlur}
    //           onChange={onChange}
    //           value={[]}
    //           style={{width: '100%'}}
    //           placeholder="Select contact"
    //           allowClear={true}
    //           notFoundContent={!options.length ? 'Add a "Contact" type Metadata to enable notifications' : 'No field matches your request'}>
    //     {options}
    //   </Select>
    // );

  }

  switcher(props) {
    return <Switch size="small" onChange={props.input.onChange} checked={!!props.input.value}/>;
  }

  render() {

    return (
      <FormItem>
        <Item offset="small">
          <Field name={`${this.props.field.get('fieldPrefix')}.isNotificationsEnabled`} component={this.switcher}/>
          {this.props.field.get('isNotificationsEnabled') && (
            <span className="product-events-notifications-label">Notifications On</span>
          ) || (
            <span className="product-events-notifications-label">Notifications Off</span>
          )}
        </Item>
        <FormItem visible={!!this.props.field && !!this.props.field.get('isNotificationsEnabled')}>
          <Item label="E-mail to" offset="normal">
            <Field name={`${this.props.field.get('fieldPrefix')}.emailNotifications`}
                   component={this.notificationSelect}
                   onFocus={this.props.onFocus} onBlur={this.props.onBlur}/>
          </Item>
          {/*<Item label="PUSH Notifications to">*/}
          {/*<Field name="pushNotifications"*/}
          {/*component={this.notificationSelect}*/}
          {/*onFocus={this.props.onFocus} onBlur={this.props.onBlur}/>*/}
          {/*</Item>*/}
        </FormItem>
      </FormItem>
    );
  }

}

Notifications.Static = Static;
export default Notifications;
