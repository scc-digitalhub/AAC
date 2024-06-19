import { Typography } from '@mui/material';
import {
    EditButton,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';

export const UserShow = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    return (
        <Show queryOptions={options} actions={<ShowToolBarActions />}>
            <UserTabComponent />
        </Show>
    );
};

const UserTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <br />
            <Typography variant="h5" sx={{ ml: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.username}
            </Typography>
            <Typography variant="h6" sx={{ ml: 2 }}>
                {record.id}
            </Typography>
            <br />
            <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="username" />
                    <TextField source="email" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Acccount">
                    <TextField source="id" />
                    <TextField source="email" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Audit">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Apps">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Groups">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Roles">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Attributes">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Space Roles">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Tos">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    const params = useParams();
    const realmId = params.realmId;
    const record = useRecordContext();
    if (!record) return null;
    const to = `/users/r/${realmId}/${record.id}/edit`;
    return (
        <TopToolbar>
            <>
                <EditButton to={to}></EditButton>
            </>
        </TopToolbar>
    );
};
