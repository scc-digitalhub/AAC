import {
    EditButton,
    ShowButton,
    useRecordContext,
    useResourceContext,
    useResourceDefinition,
} from 'react-admin';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { DropDownButton } from './DropdownButton';
import { RowButtonGroup } from './RowButtonGroup';

export const ActionsButtons = () => {
    const record = useRecordContext();
    const resource = useResourceContext();
    const definition = useResourceDefinition();

    if (!record) {
        return null;
    }
    return (
        <RowButtonGroup label="⋮">
            <DropDownButton>
                {definition && definition.hasShow && <ShowButton />}
                {definition && definition.hasEdit && <EditButton />}
                <DeleteWithDialogButton />
            </DropDownButton>
        </RowButtonGroup>
    );
};
