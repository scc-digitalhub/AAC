import { Typography } from '@mui/material';
import {
    EditButton,
    RichTextField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { IdField } from '../components/IdField';

export const GroupShow = () => {
    return (
        <Show actions={<ShowToolBarActions />}>
            <GroupTabComponent />
        </Show>
    );
};

const GroupTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <br />
            <Typography variant="h5" >
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ ml: 2 }}>
                <IdField source="id" />
            </Typography>
            <br />
            <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="group" />
                    <TextField source="members" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Roles">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Members">
                    <TextField source="id" />
                    <TextField source="members" />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    return (
        <TopToolbar>
            <>
                <EditButton></EditButton>
            </>
        </TopToolbar>
    );
};
