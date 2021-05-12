import { ControlLoopDefinition } from './cl-definition.model';
import { ControlLoopState } from './enums/cl-state.enum';
import { ControlLoopOrderedState } from './enums/cl-ordered-state.enum';
import { ControlLoopElementObj } from './cl-element-obj.model';

export class ControlLoop {
  public name: string;
  public version: string;
  public definition: ControlLoopDefinition;
  public state: ControlLoopState;
  public orderedState: ControlLoopOrderedState;
  public description: string;
  public elements: ControlLoopElementObj;
}
