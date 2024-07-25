import { EditButton, ShowButton, useRecordContext } from 'react-admin';
import ContentAdd from '@mui/icons-material/Add';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { DropDownButton } from './DropdownButton';
import { RowButtonGroup } from './RowButtonGroup';


export const ActionsButtons = () => {
    const record = useRecordContext();
    if (!record) {
        return null;
    }
    return (
        <RowButtonGroup label="⋮">
        <DropDownButton>
            <ShowButton />
            <EditButton />
            <DeleteWithDialogButton />
        </DropDownButton>
     </RowButtonGroup>
    );
};

