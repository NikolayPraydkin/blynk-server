import React                    from 'react';
import {Input, Item}            from "components/UI";
import Validation               from 'services/Validation';
import {Row, Col, Switch}       from 'antd';
import {
  reduxForm,
  Field as FormField
}                               from 'redux-form';
import ImageUploader            from 'components/ImageUploader';

@reduxForm()
class Form extends React.Component {

  render() {
    return (
      <Row gutter={24}>
        <Col span={15}>

          <Item label="Name" offset="medium">
            <Input name="name" placeholder="Name" validate={[Validation.Rules.required]}/>
          </Item>

          <Item label="Description" offset="normal">
            <Input name="description" type="textarea" rows="5" placeholder="Description (optional)"/>
          </Item>

          <Item>
            <Switch size="small"/> <span className="switch-label">Organization can create Sub-Organizations</span>
          </Item>

        </Col>
        <Col span={9}>
          <div className="organizations-create-drag-and-drop">
            <FormField name="logoUrl"
                       component={({input, meta: {error, touched}}) => (
                         <ImageUploader text="Add image"
                                        logo={input.value}
                                        error={error}
                                        touched={touched}
                                        hint={() => (
                                          <span>Upload from computer or drag-n-drop<br/>.png or .jpg, min 500x500px</span>
                                        )}
                                        onChange={() => {
                                        }}/>
                       )}/>
          </div>
        </Col>
      </Row>
    );
  }

}

export default Form;
