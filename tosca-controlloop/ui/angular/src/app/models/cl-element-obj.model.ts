import { ControlLoopDefinition } from './cl-definition.model';
import { ControlLoopParticipantId } from './cl-participant-id.model';
import { ControlLoopState } from './enums/cl-state.enum';
import { ControlLoopOrderedState } from './enums/cl-ordered-state.enum';

export interface IClElModel {
  id: string;
  definition: ControlLoopDefinition;
  participantId: ControlLoopParticipantId;
  state: ControlLoopState;
  orderedState: ControlLoopOrderedState;
  description: string;
}

export class ControlLoopElementObj {
  public object: {};

  constructor( private model: IClElModel ) {
    this.object[model.id] = this.model.id;
  }
}
