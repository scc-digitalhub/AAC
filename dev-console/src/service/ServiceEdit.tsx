import {
    Typography,
} from '@mui/material';
import {
    Edit,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';

export const ServiceEdit = () => {
    return (
        <Edit
            actions={<EditToolBarActions />}
            mutationMode="pessimistic"
        >
            <ServiceTabComponent />
        </Edit>
    );
};


const ServiceTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;


    return (
        <>
            <br />
            <Typography variant="h5" sx={{ ml: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ ml: 2 }}>
                {record.provider}
            </Typography>
            <br />
        </>
    );
};



const EditToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <InspectButton />
             <DeleteWithDialogButton/>
            <ExportRecordButton />
        </TopToolbar>
    );
};


