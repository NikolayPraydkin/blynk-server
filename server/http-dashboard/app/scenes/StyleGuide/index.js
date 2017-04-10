import React from 'react';

import {Button, Input, Checkbox} from 'antd';

import './styles.scss';

class StyleGuide extends React.Component {
  render() {
    return (
      <div className="style-guide">
        <div className="style-guide-element">
          <Button type="primary">Button</Button>
        </div>
        <div className="style-guide-element">
          <Button type="primary" icon="plus">Button</Button>
        </div>
        <div className="style-guide-element">
          <Button>Button</Button>
        </div>
        <div className="style-guide-element">
          <Button type="danger">Button</Button>
        </div>
        <div className="style-guide-element">
          <Button type="dashed" icon="plus">Add Meta data</Button>
        </div>
        <div className="style-guide-element">
          <Input placeholder="Label" style={{width: 168}}/>
        </div>
        <div className="style-guide-element">
          <Input placeholder="Label" style={{width: 168}} disabled/>
        </div>
        <div className="style-guide-element has-error">
          <Input placeholder="Label" style={{width: 168}}/>
        </div>
        <div className="style-guide-element">
          <Checkbox></Checkbox>
        </div>
        <div className="style-guide-element">
          <Checkbox>Check me</Checkbox>
        </div>
        <div className="style-guide-element">
          <Checkbox disabled>Check me</Checkbox>
        </div>
      </div>
    );
  }
}

export default StyleGuide;
