import {
    ShowBase,
    useShowContext,
    EditButton,
    useStore,
    DateField,
    FormDataConsumer,
} from 'react-admin';
import { Box, Card, CardContent, Typography } from '@mui/material';
import { Game } from '../types';

export const RealmShow = () => {
    const [gameId] = useStore('game.selected');
    const options = { meta: { gameId: gameId } };
    return (
        <></>
        // <ShowBase queryOptions={options}>
        //     <RealmShowContent />
        // </ShowBase>
    );
};

const RealmShowContent = () => {
    // const { record, isLoading } = useShowContext<Game>();
    // if (isLoading || !record) return null;
    // return (
    //     <Box mt={2} display="flex">
    //         <Box flex="1">
    //             <Card>
    //                 <CardContent>
    //                     <Box display="flex" width={630}>
    //                         <Box>
    //                             <Typography >Name: {record.name}</Typography>
    //                             { record.expiration > 0 &&
    //                                 <Box mt={2} width={630}>
    //                                      <Typography >Expiration: <DateField source="expiration" /> </Typography>
    //                                 </Box>
    //                             }
    //                             <br />
    //                             <Typography >Notify PointConcept :
    //                                 <ul>
    //                                     {record.notifyPCName.map(item => (
    //                                         <li>{item}</li>
    //                                     ))}
    //                                 </ul>
    //                             </Typography>
    //                             <Typography >Challenge choice config(CRON Expression): {record.challengeChoiceConfig.cronExpression}</Typography>
    //                         </Box>
    //                     </Box>
    //                 </CardContent>
    //             </Card>
    //         </Box>
    //         <Box>
    //             <EditButton label="Edit Game" to={`/game/${record.id}`} />
    //         </Box>
    //     </Box>
    // );
};
